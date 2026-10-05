package com.privacyguard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguard.audit.ComprehensiveAppAudit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticAuditListView(
    audits: List<ComprehensiveAppAudit>,
    isScanning: Boolean,
    onRescanClick: () -> Unit
) {
    var selectedReportAudit by remember { mutableStateOf<ComprehensiveAppAudit?>(null) }
    var filterViolationsOnly by remember { mutableStateOf(false) }

    val flaggedBreakersCount = remember(audits) { audits.count { it.hasPolicyViolations } }
    val displayedAudits = remember(audits, filterViolationsOnly) {
        if (filterViolationsOnly) {
            audits.filter { it.hasPolicyViolations }
        } else {
            audits
        }
    }

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
                    text = "App Privacy & Policy Audits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${audits.size} apps analyzed • $flaggedBreakersCount policy violations flagged",
                    fontSize = 12.sp,
                    color = if (flaggedBreakersCount > 0) Color(0xFFD32F2F) else Color.Gray
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

        // Filter Bar (All Apps vs Flagged Policy Breakers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !filterViolationsOnly,
                onClick = { filterViolationsOnly = false },
                label = { Text("All Audits (${audits.size})", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )

            FilterChip(
                selected = filterViolationsOnly,
                onClick = { filterViolationsOnly = true },
                label = {
                    Text(
                        "Flagged Breaches ($flaggedBreakersCount)",
                        fontSize = 12.sp,
                        fontWeight = if (flaggedBreakersCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (flaggedBreakersCount > 0) Color(0xFFD32F2F) else Color.Gray
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFCDD2),
                    selectedLabelColor = Color(0xFFB71C1C)
                )
            )
        }

        if (displayedAudits.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (filterViolationsOnly) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (filterViolationsOnly) "No Policy Breakers Found!" else "No Tracker SDKs Detected!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (filterViolationsOnly) "None of your installed apps violate Google Play Developer Program policies." else "Your scanned apps have zero known commercial telemetry SDKs.",
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
                items(displayedAudits, key = { it.packageName }) { audit ->
                    AppAuditCard(
                        audit = audit,
                        onReportClick = { selectedReportAudit = it }
                    )
                }
            }
        }
    }

    selectedReportAudit?.let { audit ->
        PolicyReportBottomSheet(
            audit = audit,
            onDismiss = { selectedReportAudit = null }
        )
    }
}

@Composable
fun AppAuditCard(
    audit: ComprehensiveAppAudit,
    onReportClick: (ComprehensiveAppAudit) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (audit.hasPolicyViolations) Color(0xFFFFF7F7) else if (audit.riskRating == "CRITICAL") Color(0xFFFFF0F0) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (audit.hasPolicyViolations) BorderStroke(1.dp, Color(0xFFFFCDD2)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(audit.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "${audit.packageName} • v${audit.versionName}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = when {
                        audit.hasPolicyViolations -> Color(0xFFD32F2F)
                        audit.riskRating == "CRITICAL" -> Color(0xFFD32F2F)
                        audit.riskRating == "MEDIUM" -> Color(0xFFF57C00)
                        else -> Color(0xFF388E3C)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (audit.hasPolicyViolations) "${audit.policyViolations.size} VIOLATIONS" else "${audit.embeddedTrackers.size} TRACKERS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Prominent Policy Violations Section
            if (audit.policyViolations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Gavel,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Play Policy Breaches (${audit.policyViolations.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB71C1C)
                            )
                        }

                        audit.policyViolations.forEach { violation ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${violation.title}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF880E4F),
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = Color(violation.severity.colorHex),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = violation.severity.label,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = violation.observedBehavior,
                                fontSize = 10.sp,
                                color = Color(0xFF4E342E),
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Embedded Trackers Section
            if (audit.embeddedTrackers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "📡 Embedded Commercial Tracking SDKs (${audit.embeddedTrackers.size}):",
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

            // Suspicious Permissions Section
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

            // Reporting Action Button
            Spacer(modifier = Modifier.height(12.dp))
            if (audit.hasPolicyViolations) {
                Button(
                    onClick = { onReportClick(audit) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1-Tap Prepare & Report to Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onReportClick(audit) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Report App Policy Violation...",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
