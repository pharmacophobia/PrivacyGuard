package com.privacyguard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveTelemetryFeedView(
    events: List<TelemetryEvent>,
    filterBlockedOnly: Boolean,
    onToggleFilterBlocked: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearEvents: () -> Unit,
    onEventClick: (TelemetryEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by app, package, or domain...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !filterBlockedOnly,
                        onClick = { onToggleFilterBlocked(false) },
                        label = { Text("All Traffic", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = filterBlockedOnly,
                        onClick = { onToggleFilterBlocked(true) },
                        leadingIcon = {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text("Blocked Only", fontSize = 12.sp) }
                    )
                }

                if (events.isNotEmpty()) {
                    TextButton(
                        onClick = onClearEvents,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
        }

        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (filterBlockedOnly) "No blocked telemetry matching filter." else "No network traffic captured yet.",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Activate the SDK Shield and launch installed apps to watch trackers get intercepted.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    TelemetryEventCard(event = event, onClick = { onEventClick(event) })
                }
            }
        }
    }
}

@Composable
fun TelemetryEventCard(
    event: TelemetryEvent,
    onClick: () -> Unit
) {
    val isBlocked = event.action == TrafficAction.BLOCKED

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isBlocked) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isBlocked) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = "Blocked",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = event.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                ActionBadge(action = event.action)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = event.domain,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isBlocked) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.packageName,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = event.formattedTime,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ActionBadge(action: TrafficAction) {
    val (label, bg, fg) = when (action) {
        TrafficAction.ALLOWED -> Triple("CLEAN / ALLOWED", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        TrafficAction.BLOCKED -> Triple("BLOCKED (0.0.0.0)", Color(0xFFFFEBEE), Color(0xFFC62828))
        TrafficAction.SIMULATED -> Triple("SIMULATED DATA", Color(0xFFE3F2FD), Color(0xFF1565C0))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
