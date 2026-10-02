package com.privacyguard.ui

import com.privacyguard.audit.ComprehensiveAppAudit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TrafficAction {
    ALLOWED, BLOCKED, SIMULATED
}

data class TelemetryEvent(
    val id: String,
    val packageName: String,
    val appName: String,
    val domain: String,
    val ipAddress: String,
    val timestamp: Long,
    val action: TrafficAction,
    val capturedPayload: String?,
    val headers: Map<String, String>,
    val syntheticResponse: String?
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

data class DashboardUiState(
    val isVpnActive: Boolean = false,
    val telemetryEvents: List<TelemetryEvent> = emptyList(),
    val appAudits: List<ComprehensiveAppAudit> = emptyList(),
    val selectedEvent: TelemetryEvent? = null,
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val filterBlockedOnly: Boolean = false,
    val totalBlockedCount: Int = 0,
    val totalAllowedCount: Int = 0,
    val uniqueTrackersBlocked: Set<String> = emptySet(),
    val isScanning: Boolean = false
) {
    val totalQueriesCount: Int
        get() = totalBlockedCount + totalAllowedCount

    val filteredEvents: List<TelemetryEvent>
        get() {
            var list = telemetryEvents
            if (filterBlockedOnly) {
                list = list.filter { it.action == TrafficAction.BLOCKED }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase().trim()
                list = list.filter {
                    it.appName.lowercase().contains(q) ||
                    it.packageName.lowercase().contains(q) ||
                    it.domain.lowercase().contains(q)
                }
            }
            return list
        }
}
