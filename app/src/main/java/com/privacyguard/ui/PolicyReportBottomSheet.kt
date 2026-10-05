package com.privacyguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyguard.audit.ComprehensiveAppAudit
import com.privacyguard.audit.PolicyReportFormatter
import com.privacyguard.reporting.PolicyReportingDispatcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyReportBottomSheet(
    audit: ComprehensiveAppAudit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reportText = remember(audit) { PolicyReportFormatter.generateComplaintBrief(audit) }
    var showFullBrief by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Policy Violation Report",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${audit.appName} (${audit.packageName})",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = Color(0xFFD32F2F),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${audit.policyViolations.size} BREACHES",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guidance Banner
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google Play requires human confirmation for reports. Tap below to automatically copy structured evidence and open Google's reporting destination.",
                        fontSize = 12.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of Violations
            Text(
                text = "Detected Policy Violations:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            audit.policyViolations.forEach { violation ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = violation.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF212121),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = Color(violation.severity.colorHex),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = violation.severity.label,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = violation.observedBehavior,
                            fontSize = 11.sp,
                            color = Color(0xFF424242),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⚖️ ${violation.policyBreachClause}",
                                fontSize = 10.sp,
                                color = Color(0xFF616161),
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        if (violation.mismatchedPermissions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Permissions:", fontSize = 10.sp, color = Color.Gray)
                                violation.mismatchedPermissions.forEach { perm ->
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = perm,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFC62828),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Technical Evidence Preview Collapsible
            OutlinedButton(
                onClick = { showFullBrief = !showFullBrief },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    if (showFullBrief) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showFullBrief) "Hide Evidence Brief" else "Preview Formatted Legal Brief",
                    fontSize = 12.sp
                )
            }

            if (showFullBrief) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reportText,
                        color = Color(0xFFB0BEC5),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary 1-Tap Action: Google Play Store
            Button(
                onClick = {
                    PolicyReportingDispatcher.launchPlayStoreReporting(context, audit, reportText)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01875F)), // Official Google Play Green
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Shop, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "1-Tap Report in Google Play",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Copies evidence & opens Play Store listing to flag",
                        fontSize = 10.sp,
                        color = Color(0xFFE8F5E9)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Action: Google Web Takedown Form
            FilledTonalButton(
                onClick = {
                    PolicyReportingDispatcher.launchWebTakedownForm(context, audit, reportText)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Open Google Policy Takedown Web Form",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "support.google.com/googleplay/android-developer/contact/takedown",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: Share and Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        PolicyReportingDispatcher.copyEvidenceToClipboard(context, reportText)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Brief", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        PolicyReportingDispatcher.shareReport(context, audit, reportText)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Evidence", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
