package com.privacyguard.audit

import android.content.pm.PackageInfo
import com.privacyguard.database.TrackerCategory

object PolicyViolationDetector {

    private val AD_AND_BROKER_CATEGORIES = setOf(
        TrackerCategory.ADVERTISING.displayName,
        TrackerCategory.DATA_BROKER.displayName
    )

    private val ATTRIBUTION_CATEGORIES = setOf(
        TrackerCategory.ATTRIBUTION.displayName,
        TrackerCategory.DATA_BROKER.displayName
    )

    fun evaluateViolations(
        pkg: PackageInfo,
        appName: String,
        declaredPerms: List<String>,
        grantedPerms: List<String>,
        trackers: List<DetectedTracker>
    ): List<PolicyViolation> {
        val violations = mutableListOf<PolicyViolation>()
        val lowerName = appName.lowercase()
        val pkgName = pkg.packageName.lowercase()

        val isNavigationOrMap = lowerName.contains("map") || lowerName.contains("nav") ||
                lowerName.contains("gps") || lowerName.contains("weather") ||
                pkgName.contains("maps") || pkgName.contains("navigation")

        val isUtility = lowerName.contains("calculator") || lowerName.contains("flashlight") ||
                lowerName.contains("torch") || lowerName.contains("clock") ||
                lowerName.contains("timer") || lowerName.contains("battery") ||
                lowerName.contains("cleaner") || lowerName.contains("compass") ||
                lowerName.contains("wallpaper")

        val isDefaultCommunicationApp = lowerName.contains("dialer") || lowerName.contains("phone") ||
                lowerName.contains("sms") || lowerName.contains("messenger") ||
                pkgName.contains("dialer") || pkgName.contains("messaging")

        val adTrackers = trackers.filter { it.category in AD_AND_BROKER_CATEGORIES }
        val attributionTrackers = trackers.filter { it.category in ATTRIBUTION_CATEGORIES }

        // 1. Location Exfiltration via Ad SDKs (User Data & Location Policy)
        val locationPerms = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ).filter { it in declaredPerms }

        if (locationPerms.isNotEmpty() && adTrackers.isNotEmpty() && !isNavigationOrMap) {
            val hasFine = android.Manifest.permission.ACCESS_FINE_LOCATION in locationPerms
            violations.add(
                PolicyViolation(
                    ruleId = "RULE_LOCATION_AD_HARVESTING",
                    title = "Ad Network Precise Location Exfiltration",
                    severity = if (hasFine) ViolationSeverity.CRITICAL else ViolationSeverity.HIGH,
                    category = PolicyCategory.USER_DATA_LOCATION,
                    offendingSdks = adTrackers,
                    mismatchedPermissions = locationPerms.map { it.substringAfterLast(".") },
                    observedBehavior = "Non-navigation app requests precise geolocation permissions while integrating commercial ad SDKs (${adTrackers.joinToString { it.name }}).",
                    policyBreachClause = "Google Play User Data & Ads Policy: Apps must not request location access solely for ad targeting. Ad networks must not collect precise user geolocation without prominent in-app disclosure.",
                    googlePlayPolicySection = "User Data Policy: Geolocation & Sensitive Device Information",
                    recommendedTakedownReason = "User Data Policy Violation (Location Exfiltration by Ad SDKs)"
                )
            )
        }

        // 2. Background Location Abuse
        if (android.Manifest.permission.ACCESS_BACKGROUND_LOCATION in declaredPerms &&
            (adTrackers.isNotEmpty() || !isNavigationOrMap)
        ) {
            violations.add(
                PolicyViolation(
                    ruleId = "RULE_BACKGROUND_LOCATION_ABUSE",
                    title = "Unauthorized Background Location Access",
                    severity = ViolationSeverity.CRITICAL,
                    category = PolicyCategory.BACKGROUND_LOCATION,
                    offendingSdks = adTrackers.ifEmpty { trackers },
                    mismatchedPermissions = listOf("ACCESS_BACKGROUND_LOCATION"),
                    observedBehavior = "App requests continuous background location tracking without clear functional necessity, exposing user location history to third-party SDKs.",
                    policyBreachClause = "Google Play Background Location Policy: Background location is strictly limited to core app features. Requesting background location when ad/telemetry SDKs are active violates Google Play mandates.",
                    googlePlayPolicySection = "Location Permissions Policy: Background Location Criteria",
                    recommendedTakedownReason = "Background Location Policy Non-Compliance"
                )
            )
        }

        // 3. Restricted SMS & Call Log Access
        val commPerms = listOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG
        ).filter { it in declaredPerms }

        if (commPerms.isNotEmpty() && !isDefaultCommunicationApp) {
            violations.add(
                PolicyViolation(
                    ruleId = "RULE_RESTRICTED_COMM_PERMS",
                    title = "Restricted SMS / Call Log Permission Harvesting",
                    severity = ViolationSeverity.CRITICAL,
                    category = PolicyCategory.SENSITIVE_PERMISSIONS,
                    offendingSdks = trackers,
                    mismatchedPermissions = commPerms.map { it.substringAfterLast(".") },
                    observedBehavior = "App requests restricted SMS or Call Log permissions despite not being the device's default telephony/SMS client.",
                    policyBreachClause = "Google Play Permissions Policy: Call Log and SMS permissions are strictly restricted to apps designated as default handlers. Access to user communication records is prohibited for secondary purposes.",
                    googlePlayPolicySection = "Permissions Policy: SMS and Call Log Core Functionality",
                    recommendedTakedownReason = "Restricted Permissions Breach (Unauthorized SMS/Call Log Access)"
                )
            )
        }

        // 4. Deceptive Sensor Access in Utility Apps
        val sensorPerms = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        ).filter { it in declaredPerms }

        if (sensorPerms.isNotEmpty() && isUtility) {
            violations.add(
                PolicyViolation(
                    ruleId = "RULE_DECEPTIVE_SENSOR_ACCESS",
                    title = "Deceptive Sensor Access (Mic/Camera in Utility)",
                    severity = ViolationSeverity.CRITICAL,
                    category = PolicyCategory.DEVICE_SENSOR_SURVEILLANCE,
                    offendingSdks = trackers,
                    mismatchedPermissions = sensorPerms.map { it.substringAfterLast(".") },
                    observedBehavior = "Simple utility application ($appName) declares audio recording or camera access with no functional correlation to its basic utility toolset.",
                    policyBreachClause = "Google Play Device & Network Abuse / Deceptive Behavior: Apps must not access microphone or camera sensors without transparent, user-facing core justification.",
                    googlePlayPolicySection = "Deceptive Behavior & Device Abuse Policy",
                    recommendedTakedownReason = "Deceptive Behavior & Unauthorized Sensor Access"
                )
            )
        }

        // 5. Contacts Harvesting with Commercial Ad Networks
        if (android.Manifest.permission.READ_CONTACTS in declaredPerms && adTrackers.isNotEmpty()) {
            violations.add(
                PolicyViolation(
                    ruleId = "RULE_CONTACTS_AD_HARVESTING",
                    title = "Address Book Harvesting with Ad Networks",
                    severity = ViolationSeverity.HIGH,
                    category = PolicyCategory.SENSITIVE_PERMISSIONS,
                    offendingSdks = adTrackers,
                    mismatchedPermissions = listOf("READ_CONTACTS"),
                    observedBehavior = "App requests full address book contacts while containing commercial ad SDKs (${adTrackers.joinToString { it.name }}).",
                    policyBreachClause = "Google Play User Data Policy: Personal contact lists must never be disclosed, harvested, or transmitted to third-party ad networks or data brokers.",
                    googlePlayPolicySection = "User Data Policy: Personal and Sensitive Information",
                    recommendedTakedownReason = "User Data Policy Violation (Contact List Sharing with Ad SDKs)"
                )
            )
        }

        // 6. Persistent Device Fingerprinting & Data Brokerage
        val phoneStatePerms = listOf(
            android.Manifest.permission.READ_PHONE_STATE
        ).filter { it in declaredPerms }

        if (phoneStatePerms.isNotEmpty() && attributionTrackers.isNotEmpty()) {
            violations.add(
                PolicyViolation(
                    ruleId = "RULE_DEVICE_FINGERPRINTING",
                    title = "Undeclared Hardware Fingerprinting & Telemetry",
                    severity = ViolationSeverity.HIGH,
                    category = PolicyCategory.DATA_SAFETY_FINGERPRINTING,
                    offendingSdks = attributionTrackers,
                    mismatchedPermissions = listOf("READ_PHONE_STATE"),
                    observedBehavior = "App integrates attribution/data broker engines alongside telephony state permissions, enabling hardware-level cross-app device fingerprinting.",
                    policyBreachClause = "Google Play Data Safety & Device Identifiers Policy: Persistent hardware identifiers (IMEI, IMSI, serials) must not be correlated with cross-app tracking without user consent.",
                    googlePlayPolicySection = "Data Safety Policy: Device Identification & Fingerprinting",
                    recommendedTakedownReason = "Data Safety Non-Compliance (Persistent Hardware Tracking)"
                )
            )
        }

        return violations
    }
}
