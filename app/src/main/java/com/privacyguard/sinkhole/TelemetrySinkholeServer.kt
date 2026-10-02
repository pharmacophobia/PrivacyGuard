package com.privacyguard.sinkhole

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class TelemetrySinkholeServer(port: Int = 8080) : NanoHTTPD(port) {

    data class CapturedPayload(
        val timestamp: Long,
        val path: String,
        val headers: Map<String, String>,
        val rawBody: String
    )

    private val capturedLogs = mutableListOf<CapturedPayload>()

    override fun serve(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        try {
            session.parseBody(files)
        } catch (e: Exception) {
            // Log parse error
        }

        val postData = files["postData"] ?: ""

        synchronized(capturedLogs) {
            capturedLogs.add(
                CapturedPayload(
                    timestamp = System.currentTimeMillis(),
                    path = session.uri,
                    headers = session.headers,
                    rawBody = postData
                )
            )
        }

        val syntheticResponse = JSONObject().apply {
            put("status", "success")
            put("code", 200)
            put("recorded_events", 1)
            put("simulated", true)
        }.toString()

        val hostHeader = session.headers["host"] ?: "telemetry.sinkhole.local"
        val userAgent = session.headers["user-agent"] ?: "Unknown Client"
        val event = com.privacyguard.ui.TelemetryEvent(
            id = java.util.UUID.randomUUID().toString(),
            packageName = "com.privacyguard.sinkhole",
            appName = "Captured ($userAgent)",
            domain = hostHeader + session.uri,
            ipAddress = session.remoteIpAddress ?: "127.0.0.1",
            timestamp = System.currentTimeMillis(),
            action = com.privacyguard.ui.TrafficAction.SIMULATED,
            capturedPayload = postData.ifEmpty { session.queryParameterString ?: "(Empty Body)" },
            headers = session.headers,
            syntheticResponse = syntheticResponse
        )
        com.privacyguard.traffic.TelemetryEventHub.emitEvent(event)

        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            syntheticResponse
        )
    }

    fun getCapturedTelemetry(): List<CapturedPayload> = synchronized(capturedLogs) {
        capturedLogs.toList()
    }
}
