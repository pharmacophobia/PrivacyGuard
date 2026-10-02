package com.privacyguard.audit

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.privacyguard.database.TrackerDatabase

data class DetectedTracker(
    val name: String,
    val category: String,
    val company: String,
    val description: String,
    val signature: String
)

data class ComprehensiveAppAudit(
    val packageName: String,
    val appName: String,
    val suspiciousPermissions: List<String>,
    val embeddedTrackers: List<DetectedTracker>,
    val riskRating: String // "LOW", "MEDIUM", "CRITICAL"
)

class StaticPrivacyScanner(private val context: Context) {

    fun scanAllApps(): List<ComprehensiveAppAudit> {
        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_PROVIDERS
        val apps = pm.getInstalledPackages(flags)

        val results = mutableListOf<ComprehensiveAppAudit>()

        for (pkg in apps) {
            val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            if (isSystem) continue

            val appName = pkg.applicationInfo?.loadLabel(pm).toString()
            val suspiciousPerms = findAnomalousPermissions(pkg, appName)
            val trackers = findEmbeddedTrackers(pkg)

            if (suspiciousPerms.isNotEmpty() || trackers.isNotEmpty()) {
                val rating = when {
                    suspiciousPerms.size >= 3 || trackers.size >= 4 -> "CRITICAL"
                    suspiciousPerms.isNotEmpty() || trackers.size >= 2 -> "MEDIUM"
                    else -> "LOW"
                }

                results.add(
                    ComprehensiveAppAudit(
                        packageName = pkg.packageName,
                        appName = appName,
                        suspiciousPermissions = suspiciousPerms,
                        embeddedTrackers = trackers,
                        riskRating = rating
                    )
                )
            }
        }
        return results.sortedWith(
            compareByDescending<ComprehensiveAppAudit> { it.embeddedTrackers.size }
                .thenByDescending { it.riskRating == "CRITICAL" }
        )
    }

    private fun findAnomalousPermissions(pkg: PackageInfo, appName: String): List<String> {
        val anomalies = mutableListOf<String>()
        val criticalPerms = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_SMS
        )

        val name = appName.lowercase()
        val isUtility = name.contains("calculator") || name.contains("flashlight") ||
                name.contains("torch") || name.contains("clock") || name.contains("timer")

        pkg.requestedPermissions?.forEachIndexed { index, perm ->
            val isGranted = (pkg.requestedPermissionsFlags?.getOrNull(index) ?: 0) and
                    PackageInfo.REQUESTED_PERMISSION_GRANTED != 0

            if (isGranted && perm in criticalPerms) {
                if (isUtility) {
                    anomalies.add(perm)
                }
            }
        }
        return anomalies
    }

    private fun findEmbeddedTrackers(pkg: PackageInfo): List<DetectedTracker> {
        val detected = mutableListOf<DetectedTracker>()
        val components = mutableListOf<String>()

        pkg.services?.forEach { components.add(it.name) }
        pkg.receivers?.forEach { components.add(it.name) }
        pkg.activities?.forEach { components.add(it.name) }
        pkg.providers?.forEach { components.add(it.name) }

        val allTrackers = TrackerDatabase.getAllTrackers()
        for (tracker in allTrackers) {
            for (sig in tracker.codeSignatures) {
                if (components.any { it.startsWith(sig) || it.contains(sig) }) {
                    detected.add(
                        DetectedTracker(
                            name = tracker.name,
                            category = tracker.category.displayName,
                            company = tracker.company,
                            description = tracker.description,
                            signature = sig
                        )
                    )
                    break
                }
            }
        }
        return detected.distinctBy { it.name }
    }
}
