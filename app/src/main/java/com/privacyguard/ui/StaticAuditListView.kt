package com.privacyguard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguard.audit.ComprehensiveAppAudit

@Composable
fun StaticAuditListView(
    audits: List<ComprehensiveAppAudit>,
    isScanning: Boolean,
    onRescanClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Privacy Audits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${audits.size} apps analyzed for embedded tracking SDKs",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            FilledTonalButton(
                onClick = onRescanClick,
                enabled = !isScanning,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanning...", fontSize = 12.sp)
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Re-scan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-scan", fontSize = 12.sp)
                }
            }
        }

        if (audits.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No Tracker SDKs Detected!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your scanned apps have zero known commercial telemetry SDKs.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(audits, key = { it.packageName }) { audit ->
                    AppAuditCard(audit = audit)
                }
            }
        }
    }
}

@Composable
fun AppAuditCard(audit: ComprehensiveAppAudit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (audit.riskRating == "CRITICAL") Color(0xFFFFF0F0) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(audit.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(audit.packageName, fontSize = 11.sp, color = Color.Gray)
                }

                Surface(
                    color = when (audit.riskRating) {
                        "CRITICAL" -> Color(0xFFD32F2F)
                        "MEDIUM" -> Color(0xFFF57C00)
                        else -> Color(0xFF388E3C)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${audit.embeddedTrackers.size} TRACKERS (${audit.riskRating})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (audit.embeddedTrackers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "📡 Embedded Commercial Tracking SDKs:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                audit.embeddedTrackers.forEach { tracker ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "• ${tracker.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF212121)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tracker.category,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "  Vendor: ${tracker.company} | ${tracker.description}",
                            fontSize = 10.sp,
                            color = Color(0xFF616161)
                        )
                    }
                }
            }

            if (audit.suspiciousPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🚨 Suspicious / Sensitive Permissions:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(0xFFB71C1C)
                )
                audit.suspiciousPermissions.forEach { perm ->
                    Text(
                        text = "• ${perm.substringAfterLast(".")}",
                        fontSize = 11.sp,
                        color = Color(0xFF424242),
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}
