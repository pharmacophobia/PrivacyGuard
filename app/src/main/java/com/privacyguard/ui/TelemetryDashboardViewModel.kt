package com.privacyguard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyguard.audit.StaticPrivacyScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TelemetryDashboardViewModel(
    private val staticScanner: StaticPrivacyScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadStaticAudits()
        viewModelScope.launch {
            com.privacyguard.traffic.TelemetryEventHub.events.collect { event ->
                _uiState.update { state ->
                    val isBlocked = event.action == TrafficAction.BLOCKED
                    val trackerName = if (isBlocked && event.domain.contains("[")) {
                        event.domain.substringAfter("[").substringBefore("]")
                    } else null

                    val updatedUniqueTrackers = if (trackerName != null) {
                        state.uniqueTrackersBlocked + trackerName
                    } else {
                        state.uniqueTrackersBlocked
                    }

                    state.copy(
                        telemetryEvents = listOf(event) + state.telemetryEvents.take(499),
                        totalBlockedCount = if (isBlocked) state.totalBlockedCount + 1 else state.totalBlockedCount,
                        totalAllowedCount = if (!isBlocked) state.totalAllowedCount + 1 else state.totalAllowedCount,
                        uniqueTrackersBlocked = updatedUniqueTrackers
                    )
                }
            }
        }
    }

    fun toggleVpn(enabled: Boolean) {
        _uiState.update { it.copy(isVpnActive = enabled) }
    }

    fun switchTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectEvent(event: TelemetryEvent?) {
        _uiState.update { it.copy(selectedEvent = event) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFilterBlockedOnly(blockedOnly: Boolean) {
        _uiState.update { it.copy(filterBlockedOnly = blockedOnly) }
    }

    fun clearEvents() {
        _uiState.update {
            it.copy(
                telemetryEvents = emptyList(),
                totalBlockedCount = 0,
                totalAllowedCount = 0,
                uniqueTrackersBlocked = emptySet()
            )
        }
    }

    fun loadStaticAudits() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isScanning = true) }
            try {
                val audits = staticScanner.scanAllApps()
                _uiState.update { it.copy(appAudits = audits, isScanning = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun onNewTelemetryIntercepted(
        packageName: String,
        appName: String,
        domain: String,
        ip: String,
        action: TrafficAction,
        rawBody: String?,
        headers: Map<String, String>
    ) {
        val event = TelemetryEvent(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            domain = domain,
            ipAddress = ip,
            timestamp = System.currentTimeMillis(),
            action = action,
            capturedPayload = rawBody,
            headers = headers,
            syntheticResponse = if (action == TrafficAction.SIMULATED) {
                """{"status": "success", "code": 200, "simulated": true}"""
            } else null
        )

        com.privacyguard.traffic.TelemetryEventHub.emitEvent(event)
    }
}
