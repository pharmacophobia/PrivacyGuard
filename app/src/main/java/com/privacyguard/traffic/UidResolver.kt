package com.privacyguard.traffic

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.net.InetSocketAddress

class UidResolver(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun resolveAppUid(
        protocol: Int,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ): Int? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null

        val local = InetSocketAddress(srcIp, srcPort)
        val remote = InetSocketAddress(dstIp, dstPort)

        return try {
            cm.getConnectionOwnerUid(protocol, local, remote)
        } catch (e: Exception) {
            null
        }
    }

    fun getPackageNameFromUid(uid: Int): String? {
        return context.packageManager.getPackagesForUid(uid)?.firstOrNull()
    }
}
