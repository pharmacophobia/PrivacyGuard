package com.privacyguard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadInspectorBottomSheet(
    event: TelemetryEvent,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Telemetry Inspector", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${event.appName} → ${event.domain}", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Intercepted Outbound Body:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            CodeBlock(
                code = event.capturedPayload?.ifEmpty { "(No plaintext payload body captured)" }
                    ?: "(Encrypted TLS Stream - Hostname & Metrics intercepted via SNI)"
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (event.action == TrafficAction.SIMULATED) {
                Text("Simulated Response Sent to App:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1976D2))
                Spacer(modifier = Modifier.height(4.dp))
                CodeBlock(code = event.syntheticResponse ?: "HTTP/1.1 200 OK")
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text("Request Headers:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            val headerText = if (event.headers.isNotEmpty()) {
                event.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            } else {
                "Host: ${event.domain}\nDestination-IP: ${event.ipAddress}"
            }
            CodeBlock(code = headerText)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CodeBlock(code: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = code,
                color = Color(0xFF4AF626),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}
