package com.privacyguard.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.privacyguard.MainActivity
import com.privacyguard.database.TrackerDatabase
import com.privacyguard.sinkhole.TelemetrySinkholeServer
import com.privacyguard.traffic.DnsParser
import com.privacyguard.traffic.DnsQuery
import com.privacyguard.traffic.SniExtractor
import com.privacyguard.traffic.TelemetryEventHub
import com.privacyguard.traffic.UidResolver
import com.privacyguard.ui.TelemetryEvent
import com.privacyguard.ui.TrafficAction
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class TelemetryMonitorVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private var sinkholeServer: TelemetrySinkholeServer? = null
    private var dnsForwardSocket: DatagramSocket? = null
    private lateinit var uidResolver: UidResolver

    companion object {
        private const val CHANNEL_ID = "privacy_guard_vpn"
        private const val NOTIF_ID = 1001
        private const val SINKHOLE_PORT = 8080
        private val PRIMARY_DNS = InetAddress.getByName("1.1.1.1")
        private val FALLBACK_DNS = InetAddress.getByName("8.8.8.8")
    }

    override fun onCreate() {
        super.onCreate()
        uidResolver = UidResolver(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning.get()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIF_ID,
                        buildNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIF_ID, buildNotification())
                }
                startVpn()
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        isRunning.set(true)

        try {
            sinkholeServer = TelemetrySinkholeServer(SINKHOLE_PORT)
            sinkholeServer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val forwardSocket = DatagramSocket().apply {
                soTimeout = 2500
            }
            protect(forwardSocket)
            dnsForwardSocket = forwardSocket
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Configure VPN to intercept DNS traffic cleanly without blackholing internet
        val builder = Builder()
            .setSession("PrivacyGuardVPN")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("10.0.0.2")
            .addRoute("10.0.0.0", 24)
            .setMtu(1500)
            .setBlocking(true)

        try {
            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return
        }

        if (vpnInterface == null) {
            stopSelf()
            return
        }

        workerThread = Thread {
            val vpnFd = vpnInterface?.fileDescriptor ?: return@Thread
            val inputStream = FileInputStream(vpnFd)
            val outputStream = FileOutputStream(vpnFd)
            val packet = ByteBuffer.allocate(32767)

            while (isRunning.get()) {
                try {
                    val length = inputStream.read(packet.array())
                    if (length > 0) {
                        packet.limit(length)
                        packet.position(0)
                        processPacket(packet, outputStream, length)
                        packet.clear()
                    }
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                }
            }
        }.apply { start() }
    }

    private fun processPacket(packet: ByteBuffer, outputStream: FileOutputStream, totalLength: Int) {
        if (totalLength < 20) return

        val raw = packet.array()
        val versionAndIHL = raw[0].toInt() and 0xFF
        val ipVersion = versionAndIHL ushr 4
        if (ipVersion != 4) return

        val ihl = (versionAndIHL and 0x0F) * 4
        val protocol = raw[9].toInt() and 0xFF

        val srcIp = "${raw[12].toInt() and 0xFF}.${raw[13].toInt() and 0xFF}.${raw[14].toInt() and 0xFF}.${raw[15].toInt() and 0xFF}"
        val dstIp = "${raw[16].toInt() and 0xFF}.${raw[17].toInt() and 0xFF}.${raw[18].toInt() and 0xFF}.${raw[19].toInt() and 0xFF}"

        when (protocol) {
            17 -> { // UDP (DNS)
                if (totalLength >= ihl + 8) {
                    val srcPort = ((raw[ihl].toInt() and 0xFF) shl 8) or (raw[ihl + 1].toInt() and 0xFF)
                    val dstPort = ((raw[ihl + 2].toInt() and 0xFF) shl 8) or (raw[ihl + 3].toInt() and 0xFF)

                    if (dstPort == 53) {
                        val dnsPayloadOffset = ihl + 8
                        val dnsPayloadLen = totalLength - dnsPayloadOffset
                        if (dnsPayloadLen > 12) {
                            val dnsSlice = ByteBuffer.wrap(raw, dnsPayloadOffset, dnsPayloadLen)
                            val query = DnsParser.parseQuery(dnsSlice)

                            if (query != null) {
                                handleDnsQuery(
                                    query = query,
                                    raw = raw,
                                    dnsOffset = dnsPayloadOffset,
                                    dnsLen = dnsPayloadLen,
                                    srcIp = srcIp,
                                    srcPort = srcPort,
                                    dstIp = dstIp,
                                    dstPort = dstPort,
                                    outputStream = outputStream
                                )
                            }
                        }
                    }
                }
            }
            6 -> { // TCP (TLS SNI)
                if (totalLength >= ihl + 20) {
                    val srcPort = ((raw[ihl].toInt() and 0xFF) shl 8) or (raw[ihl + 1].toInt() and 0xFF)
                    val dstPort = ((raw[ihl + 2].toInt() and 0xFF) shl 8) or (raw[ihl + 3].toInt() and 0xFF)
                    val dataOffset = ((raw[ihl + 12].toInt() and 0xF0) ushr 4) * 4

                    if (dstPort == 443 && totalLength > ihl + dataOffset) {
                        val tcpPayloadOffset = ihl + dataOffset
                        val tcpPayloadLen = totalLength - tcpPayloadOffset
                        val tcpSlice = ByteBuffer.wrap(raw, tcpPayloadOffset, tcpPayloadLen)
                        val hostname = SniExtractor.extractSni(tcpSlice)

                        if (hostname != null) {
                            handleTlsSni(hostname, srcIp, srcPort, dstIp, dstPort)
                        }
                    }
                }
            }
        }
    }

    private fun handleDnsQuery(
        query: DnsQuery,
        raw: ByteArray,
        dnsOffset: Int,
        dnsLen: Int,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        outputStream: FileOutputStream
    ) {
        val domain = query.domain
        val tracker = TrackerDatabase.matchDomain(domain)
        val isTracker = tracker != null

        val uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uidResolver.resolveAppUid(17, srcIp, srcPort, dstIp, dstPort)
        } else null

        val packageName = uid?.let { uidResolver.getPackageNameFromUid(it) } ?: "com.android.system"
        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }

        val action = if (isTracker) TrafficAction.BLOCKED else TrafficAction.ALLOWED

        val event = TelemetryEvent(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            domain = if (tracker != null) "$domain [${tracker.name}]" else domain,
            ipAddress = dstIp,
            timestamp = System.currentTimeMillis(),
            action = action,
            capturedPayload = if (isTracker) {
                "🛑 BLOCKED TELEMETRY SDK TRANSMISSION\nTracker: ${tracker.name}\nCompany: ${tracker.company}\nCategory: ${tracker.category.displayName}\nPurpose: ${tracker.description}\nHost: $domain\nType: ${if (query.qType == 28) "AAAA (IPv6)" else "A (IPv4)"}"
            } else {
                "DNS Query -> $domain\nStatus: Clean Traffic -> Upstream Resolved"
            },
            headers = mapOf(
                "Protocol" to "UDP/DNS",
                "Action" to if (isTracker) "BLOCKED" else "ALLOWED",
                "Tracker" to (tracker?.name ?: "Clean Traffic"),
                "Vendor" to (tracker?.company ?: "Whitelisted Host"),
                "Source" to "$srcIp:$srcPort",
                "Destination" to "$dstIp:$dstPort"
            ),
            syntheticResponse = if (isTracker) {
                if (query.qType == 28) "Sinkhole AAAA: :: (Blocked)" else "Sinkhole A: 0.0.0.0 (Blocked)"
            } else null
        )
        TelemetryEventHub.emitEvent(event)

        if (isTracker) {
            sendDnsSinkholeResponse(raw, dnsOffset, dnsLen, srcPort, query.qType, outputStream)
        } else {
            forwardDnsUpstream(raw, dnsOffset, dnsLen, srcPort, outputStream)
        }
    }

    private fun handleTlsSni(hostname: String, srcIp: String, srcPort: Int, dstIp: String, dstPort: Int) {
        val tracker = TrackerDatabase.matchDomain(hostname)
        val isTracker = tracker != null

        val uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uidResolver.resolveAppUid(6, srcIp, srcPort, dstIp, dstPort)
        } else null

        val packageName = uid?.let { uidResolver.getPackageNameFromUid(it) } ?: "com.android.chrome"
        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }

        val event = TelemetryEvent(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            domain = if (tracker != null) "$hostname [${tracker.name}]" else hostname,
            ipAddress = dstIp,
            timestamp = System.currentTimeMillis(),
            action = if (isTracker) TrafficAction.BLOCKED else TrafficAction.ALLOWED,
            capturedPayload = "TLS ClientHello SNI -> $hostname\nTracker: ${tracker?.name ?: "None"}\nVendor: ${tracker?.company ?: "None"}",
            headers = mapOf(
                "Protocol" to "TCP/TLS-SNI",
                "Destination" to "$dstIp:$dstPort"
            ),
            syntheticResponse = null
        )
        TelemetryEventHub.emitEvent(event)
    }

    private fun sendDnsSinkholeResponse(
        raw: ByteArray,
        dnsOffset: Int,
        dnsLen: Int,
        clientPort: Int,
        qType: Int,
        outputStream: FileOutputStream
    ) {
        try {
            val txId0 = raw[dnsOffset]
            val txId1 = raw[dnsOffset + 1]

            // Find question end (null label + 4 bytes QTYPE/QCLASS)
            var qEnd = dnsOffset + 12
            while (qEnd < dnsOffset + dnsLen && raw[qEnd].toInt() != 0) {
                val len = raw[qEnd].toInt() and 0xFF
                qEnd += 1 + len
            }
            qEnd += 5 // skip 0x00 and 4-byte QTYPE/QCLASS
            val questionLen = qEnd - (dnsOffset + 12)

            if (qType == 28) {
                // AAAA (IPv6): Return NODATA (ANCOUNT = 0) with NOERROR
                val dnsResp = ByteBuffer.allocate(12 + questionLen)
                dnsResp.put(txId0)
                dnsResp.put(txId1)
                dnsResp.putShort(0x8180.toShort()) // Response, Authoritative, Recursion Available, No Error
                dnsResp.putShort(1.toShort())      // QDCOUNT
                dnsResp.putShort(0.toShort())      // ANCOUNT = 0
                dnsResp.putShort(0.toShort())      // NSCOUNT
                dnsResp.putShort(0.toShort())      // ARCOUNT
                dnsResp.put(raw, dnsOffset + 12, questionLen)

                val respPacket = buildIpUdpPacket(
                    srcIp = byteArrayOf(10, 0, 0, 2),
                    dstIp = byteArrayOf(10, 0, 0, 2),
                    srcPort = 53,
                    dstPort = clientPort,
                    payload = dnsResp.array()
                )
                outputStream.write(respPacket)
            } else {
                // Type A (IPv4): Return 0.0.0.0 (Standard RFC Sinkhole Address)
                val dnsResp = ByteBuffer.allocate(12 + questionLen + 16)
                dnsResp.put(txId0)
                dnsResp.put(txId1)
                dnsResp.putShort(0x8180.toShort()) // Response, No error
                dnsResp.putShort(1.toShort())      // QDCOUNT
                dnsResp.putShort(1.toShort())      // ANCOUNT
                dnsResp.putShort(0.toShort())      // NSCOUNT
                dnsResp.putShort(0.toShort())      // ARCOUNT

                // Echo Question
                dnsResp.put(raw, dnsOffset + 12, questionLen)

                // Answer: 0.0.0.0
                dnsResp.putShort(0xC00C.toShort()) // Pointer to domain name in question
                dnsResp.putShort(0x0001.toShort()) // Type A
                dnsResp.putShort(0x0001.toShort()) // Class IN
                dnsResp.putInt(60)                 // TTL 60 seconds
                dnsResp.putShort(4.toShort())      // Data length 4
                dnsResp.put(0.toByte())
                dnsResp.put(0.toByte())
                dnsResp.put(0.toByte())
                dnsResp.put(0.toByte())

                val respPacket = buildIpUdpPacket(
                    srcIp = byteArrayOf(10, 0, 0, 2),
                    dstIp = byteArrayOf(10, 0, 0, 2),
                    srcPort = 53,
                    dstPort = clientPort,
                    payload = dnsResp.array()
                )
                outputStream.write(respPacket)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forwardDnsUpstream(
        raw: ByteArray,
        dnsOffset: Int,
        dnsLen: Int,
        clientPort: Int,
        outputStream: FileOutputStream
    ) {
        val socket = dnsForwardSocket ?: return
        try {
            val forwardPacket = DatagramPacket(raw, dnsOffset, dnsLen, PRIMARY_DNS, 53)
            socket.send(forwardPacket)

            val respBuf = ByteArray(1500)
            val respPacket = DatagramPacket(respBuf, respBuf.size)
            socket.receive(respPacket)

            val dnsData = respBuf.copyOf(respPacket.length)
            val fullPacket = buildIpUdpPacket(
                srcIp = byteArrayOf(10, 0, 0, 2),
                dstIp = byteArrayOf(10, 0, 0, 2),
                srcPort = 53,
                dstPort = clientPort,
                payload = dnsData
            )
            outputStream.write(fullPacket)
        } catch (e: Exception) {
            // Fallback DNS
            try {
                val fallbackPacket = DatagramPacket(raw, dnsOffset, dnsLen, FALLBACK_DNS, 53)
                socket.send(fallbackPacket)

                val respBuf = ByteArray(1500)
                val respPacket = DatagramPacket(respBuf, respBuf.size)
                socket.receive(respPacket)

                val dnsData = respBuf.copyOf(respPacket.length)
                val fullPacket = buildIpUdpPacket(
                    srcIp = byteArrayOf(10, 0, 0, 2),
                    dstIp = byteArrayOf(10, 0, 0, 2),
                    srcPort = 53,
                    dstPort = clientPort,
                    payload = dnsData
                )
                outputStream.write(fullPacket)
            } catch (e2: Exception) {
                // Timeout / drop
            }
        }
    }

    private fun buildIpUdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val packet = ByteBuffer.allocate(totalLen)

        // IP Header
        packet.put(0x45.toByte()) // Version 4, IHL 5
        packet.put(0x00.toByte()) // ToS
        packet.putShort(totalLen.toShort())
        packet.putShort(0x1234.toShort()) // Identification
        packet.putShort(0x4000.toShort()) // Don't fragment
        packet.put(64.toByte())   // TTL
        packet.put(17.toByte())   // Protocol UDP
        packet.putShort(0.toShort()) // Checksum placeholder
        packet.put(srcIp)
        packet.put(dstIp)

        // Calculate IP checksum
        val raw = packet.array()
        val checksum = calculateChecksum(raw, 0, 20)
        raw[10] = (checksum ushr 8).toByte()
        raw[11] = (checksum and 0xFF).toByte()

        // UDP Header
        packet.position(20)
        packet.putShort(srcPort.toShort())
        packet.putShort(dstPort.toShort())
        packet.putShort((8 + payload.size).toShort())
        packet.putShort(0.toShort()) // UDP checksum optional in IPv4
        packet.put(payload)

        return raw
    }

    private fun calculateChecksum(buf: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val word = ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while ((sum ushr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv()) and 0xFFFF
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Privacy Guard Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PrivacyGuard: SDK Shield Active")
            .setContentText("Commercial telemetry & tracking SDKs are blocked")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning.set(false)
        workerThread?.interrupt()
        dnsForwardSocket?.close()
        sinkholeServer?.stop()
        vpnInterface?.close()
        super.onDestroy()
    }
}
