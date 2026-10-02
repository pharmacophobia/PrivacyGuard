package com.privacyguard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.privacyguard.audit.StaticPrivacyScanner
import com.privacyguard.ui.TelemetryDashboardViewModel

class TelemetryViewModelFactory(
    private val staticScanner: StaticPrivacyScanner
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelemetryDashboardViewModel::class.java)) {
            return TelemetryDashboardViewModel(staticScanner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
