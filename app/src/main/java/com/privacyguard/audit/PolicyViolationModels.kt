package com.privacyguard.audit

enum class ViolationSeverity(val label: String, val colorHex: Long) {
    CRITICAL("CRITICAL BREACH", 0xFFD32F2F),
    HIGH("HIGH RISK", 0xFFE65100),
    MEDIUM("SUSPICIOUS", 0xFFF57C00)
}

enum class PolicyCategory(val displayName: String) {
    USER_DATA_LOCATION("User Data & Location Policy"),
    BACKGROUND_LOCATION("Background Location Abuse"),
    SENSITIVE_PERMISSIONS("Restricted Permissions Mismatch"),
    DEVICE_SENSOR_SURVEILLANCE("Undisclosed Sensor Surveillance"),
    DATA_SAFETY_FINGERPRINTING("Undeclared Device Fingerprinting"),
    DECEPTIVE_BEHAVIOR("Deceptive Utility Behavior")
}

data class PolicyViolation(
    val ruleId: String,
    val title: String,
    val severity: ViolationSeverity,
    val category: PolicyCategory,
    val offendingSdks: List<DetectedTracker>,
    val mismatchedPermissions: List<String>,
    val observedBehavior: String,
    val policyBreachClause: String,
    val googlePlayPolicySection: String,
    val recommendedTakedownReason: String
)
