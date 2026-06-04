package com.mikatechnology.BusTracker.services

import android.content.Intent
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** `trip_started` / `passenger_boarded` bildirimine basınca yolcu harita sekmesine gider. */
object PushNotificationRouter {
    private val _openPassengerMap = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openPassengerMap: SharedFlow<Unit> = _openPassengerMap.asSharedFlow()

    @Volatile
    private var pendingOpenPassengerMap = false

    fun handleIntent(intent: Intent?) {
        val type = intent?.extras?.getString("type") ?: return
        if (type == TYPE_TRIP_STARTED || type == TYPE_PASSENGER_BOARDED) {
            signalOpenPassengerMap()
        }
    }

    fun handleRemoteMessage(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        if (type == TYPE_TRIP_STARTED || type == TYPE_PASSENGER_BOARDED) {
            signalOpenPassengerMap()
        }
    }

    fun consumePendingOpenPassengerMap(): Boolean {
        if (!pendingOpenPassengerMap) return false
        pendingOpenPassengerMap = false
        return true
    }

    private fun signalOpenPassengerMap() {
        pendingOpenPassengerMap = true
        _openPassengerMap.tryEmit(Unit)
    }

    private const val TYPE_TRIP_STARTED = "trip_started"
    private const val TYPE_PASSENGER_BOARDED = "passenger_boarded"
}
