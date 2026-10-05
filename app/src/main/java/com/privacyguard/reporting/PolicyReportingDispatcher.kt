package com.privacyguard.reporting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.privacyguard.audit.ComprehensiveAppAudit

object PolicyReportingDispatcher {

    private const val PLAY_STORE_PACKAGE = "com.android.vending"
    private const val GOOGLE_TAKEDOWN_FORM_URL = "https://support.google.com/googleplay/android-developer/contact/takedown"

    fun copyEvidenceToClipboard(context: Context, reportText: String, showToast: Boolean = true): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("Google Play Policy Violation Report", reportText)
                clipboard.setPrimaryClip(clip)
                if (showToast) {
                    Toast.makeText(
                        context,
                        "📋 Evidence brief copied to clipboard!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun launchPlayStoreReporting(
        context: Context,
        audit: ComprehensiveAppAudit,
        reportText: String
    ) {
        // 1. Copy evidence payload to clipboard
        copyEvidenceToClipboard(context, reportText, showToast = false)

        // 2. Open Google Play Store listing
        val marketIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=${audit.packageName}")
            setPackage(PLAY_STORE_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (marketIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(marketIntent)
            } else {
                // Fallback to browser
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=${audit.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }

            Toast.makeText(
                context,
                "📋 Evidence copied! In Play Store, tap ⋮ (menu) -> 'Flag as inappropriate' -> paste evidence.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Google Play Store: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchWebTakedownForm(
        context: Context,
        audit: ComprehensiveAppAudit,
        reportText: String
    ) {
        // 1. Copy evidence payload to clipboard
        copyEvidenceToClipboard(context, reportText, showToast = false)

        // 2. Launch browser with pre-targeted takedown form
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(GOOGLE_TAKEDOWN_FORM_URL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            Toast.makeText(
                context,
                "📋 Technical evidence copied! Paste into Google's takedown explanation field.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareReport(
        context: Context,
        audit: ComprehensiveAppAudit,
        reportText: String
    ) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Google Play Policy Violation: ${audit.appName} (${audit.packageName})")
                putExtra(Intent.EXTRA_TEXT, reportText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Violation Evidence").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
