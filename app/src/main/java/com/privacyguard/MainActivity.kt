package com.privacyguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.privacyguard.audit.StaticPrivacyScanner
import com.privacyguard.ui.PrivacyDashboardScreen
import com.privacyguard.ui.TelemetryDashboardViewModel
import com.privacyguard.vpn.TelemetryMonitorVpnService

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<TelemetryDashboardViewModel> {
        TelemetryViewModelFactory(StaticPrivacyScanner(applicationContext))
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            viewModel.toggleVpn(false)
            Toast.makeText(this, "VPN permission required to monitor telemetry", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            PrivacyDashboardScreen(
                viewModel = viewModel,
                onStartVpnRequested = { prepareAndStartVpn() },
                onStopVpnRequested = { stopVpnService() }
            )
        }
    }

    private fun prepareAndStartVpn() {
        try {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                vpnPermissionLauncher.launch(intent)
            } else {
                startVpnService()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.toggleVpn(false)
            Toast.makeText(this, "Failed to initialize VPN: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startVpnService() {
        try {
            val serviceIntent = Intent(this, TelemetryMonitorVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            viewModel.toggleVpn(true)
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.toggleVpn(false)
            Toast.makeText(this, "Could not start service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopVpnService() {
        try {
            val serviceIntent = Intent(this, TelemetryMonitorVpnService::class.java)
            stopService(serviceIntent)
            viewModel.toggleVpn(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
