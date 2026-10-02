package com.privacyguard.traffic

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

data class DnsQuery(
    val domain: String,
    val qType: Int,  // 1 = A (IPv4), 28 = AAAA (IPv6)
    val qClass: Int
)

object DnsParser {

    fun parseQuery(dnsPayload: ByteBuffer): DnsQuery? {
        if (dnsPayload.remaining() < 12) return null

        val startPos = dnsPayload.position()
        dnsPayload.position(startPos + 4) // Skip ID & Flags
        val qdCount = dnsPayload.short.toInt() and 0xFFFF
        dnsPayload.position(startPos + 12) // Skip ANCOUNT, NSCOUNT, ARCOUNT

        if (qdCount < 1) return null

        val domainBuilder = StringBuilder()
        while (dnsPayload.hasRemaining()) {
            val labelLen = dnsPayload.get().toInt() and 0xFF
            if (labelLen == 0) break

            if (dnsPayload.remaining() < labelLen) return null
            val labelBytes = ByteArray(labelLen)
            dnsPayload.get(labelBytes)

            if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
            domainBuilder.append(String(labelBytes, StandardCharsets.UTF_8))
        }

        val domain = domainBuilder.toString().ifEmpty { return null }

        var qType = 1
        var qClass = 1
        if (dnsPayload.remaining() >= 4) {
            qType = dnsPayload.short.toInt() and 0xFFFF
            qClass = dnsPayload.short.toInt() and 0xFFFF
        }

        return DnsQuery(domain = domain, qType = qType, qClass = qClass)
    }

    fun parseQueryDomain(dnsPayload: ByteBuffer): String? {
        return parseQuery(dnsPayload)?.domain
    }
}
