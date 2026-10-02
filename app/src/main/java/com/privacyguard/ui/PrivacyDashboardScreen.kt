package com.privacyguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    viewModel: TelemetryDashboardViewModel,
    onStartVpnRequested: () -> Unit,
    onStopVpnRequested: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (state.isVpnActive) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PrivacyGuard", fontWeight = FontWeight.ExtraBold)
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = if (state.isVpnActive) "SHIELD ON" else "SHIELD OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.isVpnActive) Color(0xFF4CAF50) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = state.isVpnActive,
                            onCheckedChange = { isChecked ->
                                if (isChecked) onStartVpnRequested() else onStopVpnRequested()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live Stats Banner
            StatsBanner(
                isShieldActive = state.isVpnActive,
                blockedCount = state.totalBlockedCount,
                allowedCount = state.totalAllowedCount,
                uniqueTrackers = state.uniqueTrackersBlocked.size
            )

            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.switchTab(0) },
                    text = { Text("Live Shield (${state.filteredEvents.size})") },
                    icon = { Icon(Icons.Default.Block, contentDescription = null) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.switchTab(1) },
                    text = { Text("App Audits (${state.appAudits.size})") },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) }
                )
            }

            when (state.selectedTab) {
                0 -> LiveTelemetryFeedView(
                    events = state.filteredEvents,
                    filterBlockedOnly = state.filterBlockedOnly,
                    onToggleFilterBlocked = { viewModel.toggleFilterBlockedOnly(it) },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onClearEvents = { viewModel.clearEvents() },
                    onEventClick = { viewModel.selectEvent(it) }
                )
                1 -> StaticAuditListView(
                    audits = state.appAudits,
                    isScanning = state.isScanning,
                    onRescanClick = { viewModel.loadStaticAudits() }
                )
            }
        }

        state.selectedEvent?.let { event ->
            PayloadInspectorBottomSheet(
                event = event,
                onDismiss = { viewModel.selectEvent(null) }
            )
        }
    }
}

@Composable
fun StatsBanner(
    isShieldActive: Boolean,
    blockedCount: Int,
    allowedCount: Int,
    uniqueTrackers: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isShieldActive) Color(0xFF1B2E1E) else Color(0xFF262626)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isShieldActive) Icons.Default.Security else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isShieldActive) Color(0xFF81C784) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isShieldActive) "ACTIVE PROTECTION: ALL SDKs BLOCKED" else "SHIELD PAUSED",
                        color = if (isShieldActive) Color(0xFF81C784) else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "50+ SDK RULES",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatMetric(
                    label = "Blocked Trackers",
                    value = blockedCount.toString(),
                    color = Color(0xFFFF8A80)
                )
                VerticalDivider(
                    modifier = Modifier.height(30.dp),
                    color = Color(0x33FFFFFF)
                )
                StatMetric(
                    label = "Clean Queries",
                    value = allowedCount.toString(),
                    color = Color(0xFFA5D6A7)
                )
                VerticalDivider(
                    modifier = Modifier.height(30.dp),
                    color = Color(0x33FFFFFF)
                )
                StatMetric(
                    label = "Unique SDKs Neutralized",
                    value = uniqueTrackers.toString(),
                    color = Color(0xFFCE93D8)
                )
            }
        }
    }
}

@Composable
fun StatMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.LightGray
        )
    }
}
