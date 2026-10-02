package com.privacyguard.traffic

import com.privacyguard.ui.TelemetryEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TelemetryEventHub {

    private val _events = MutableSharedFlow<TelemetryEvent>(
        replay = 50,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<TelemetryEvent> = _events.asSharedFlow()

    fun emitEvent(event: TelemetryEvent) {
        _events.tryEmit(event)
    }
}
