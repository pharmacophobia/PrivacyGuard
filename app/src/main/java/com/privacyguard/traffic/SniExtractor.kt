package com.privacyguard.traffic

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object SniExtractor {

    fun extractSni(payload: ByteBuffer): String? {
        if (payload.remaining() < 5) return null

        val contentType = payload.get().toInt() and 0xFF
        val majorVersion = payload.get().toInt() and 0xFF
        val minorVersion = payload.get().toInt() and 0xFF
        val recordLength = payload.short.toInt() and 0xFFFF

        if (contentType != 0x16) return null
        if (payload.remaining() < recordLength) return null

        val handshakeType = payload.get().toInt() and 0xFF
        if (handshakeType != 0x01) return null

        // Skip Handshake Length (3 bytes) and Client Version (2 bytes)
        payload.position(payload.position() + 5)
        // Skip Random (32 bytes)
        if (payload.remaining() < 32) return null
        payload.position(payload.position() + 32)

        // Skip Session ID
        if (!payload.hasRemaining()) return null
        val sessionIdLen = payload.get().toInt() and 0xFF
        if (payload.remaining() < sessionIdLen) return null
        payload.position(payload.position() + sessionIdLen)

        // Skip Cipher Suites
        if (payload.remaining() < 2) return null
        val cipherSuitesLen = payload.short.toInt() and 0xFFFF
        if (payload.remaining() < cipherSuitesLen) return null
        payload.position(payload.position() + cipherSuitesLen)

        // Skip Compression Methods
        if (!payload.hasRemaining()) return null
        val compMethodsLen = payload.get().toInt() and 0xFF
        if (payload.remaining() < compMethodsLen) return null
        payload.position(payload.position() + compMethodsLen)

        // Extensions
        if (payload.remaining() < 2) return null
        val extensionsLen = payload.short.toInt() and 0xFFFF
        val extensionsEnd = payload.position() + extensionsLen
        if (payload.remaining() < extensionsLen) return null

        while (payload.position() < extensionsEnd - 4) {
            val extType = payload.short.toInt() and 0xFFFF
            val extLen = payload.short.toInt() and 0xFFFF

            if (extType == 0x0000) {
                if (payload.remaining() < extLen) return null
                val listLen = payload.short.toInt() and 0xFFFF
                val nameType = payload.get().toInt() and 0xFF
                if (nameType == 0x00) {
                    val nameLen = payload.short.toInt() and 0xFFFF
                    if (payload.remaining() < nameLen) return null
                    val nameBytes = ByteArray(nameLen)
                    payload.get(nameBytes)
                    return String(nameBytes, StandardCharsets.US_ASCII)
                }
            } else {
                if (payload.remaining() < extLen) return null
                payload.position(payload.position() + extLen)
            }
        }
        return null
    }
}
