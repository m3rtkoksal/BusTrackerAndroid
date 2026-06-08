package com.mikatechnology.BusTracker.services

import android.content.Intent
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Bildirim tıklanınca yolcu ekranında ilgili sekme / sheet açılır. */
object PushNotificationRouter {
    private val _openPassengerMap = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openPassengerMap: SharedFlow<Unit> = _openPassengerMap.asSharedFlow()

    private val _openSparseModeSheet = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openSparseModeSheet: SharedFlow<Unit> = _openSparseModeSheet.asSharedFlow()

    @Volatile
    private var pendingOpenPassengerMap = false

    @Volatile
    private var pendingOpenSparseModeSheet = false

    fun handleIntent(intent: Intent?) {
        when (intent?.extras?.getString("type")) {
            TYPE_TRIP_STARTED,
            TYPE_PASSENGER_BOARDED,
            TYPE_CANONICAL_ROUTE_READY -> signalOpenPassengerMap()
            SparseModeSuggestion.INTENT_TYPE,
            TYPE_SPARSE_MODE_SUGGESTION,
            TYPE_OPEN_HOLIDAY_MODE_LEGACY -> signalOpenSparseModeSheet()
            else -> Unit
        }
    }

    fun handleRemoteMessage(message: RemoteMessage) {
        when (message.data["type"]) {
            TYPE_TRIP_STARTED,
            TYPE_PASSENGER_BOARDED,
            TYPE_CANONICAL_ROUTE_READY -> signalOpenPassengerMap()
            TYPE_SPARSE_MODE_SUGGESTION -> signalOpenSparseModeSheet()
            else -> Unit
        }
    }

    fun consumePendingOpenPassengerMap(): Boolean {
        if (!pendingOpenPassengerMap) return false
        pendingOpenPassengerMap = false
        return true
    }

    fun consumePendingOpenSparseModeSheet(): Boolean {
        if (!pendingOpenSparseModeSheet) return false
        pendingOpenSparseModeSheet = false
        return true
    }

    private fun signalOpenPassengerMap() {
        pendingOpenPassengerMap = true
        _openPassengerMap.tryEmit(Unit)
    }

    private fun signalOpenSparseModeSheet() {
        pendingOpenSparseModeSheet = true
        _openSparseModeSheet.tryEmit(Unit)
    }

    private const val TYPE_TRIP_STARTED = "trip_started"
    private const val TYPE_PASSENGER_BOARDED = "passenger_boarded"
    private const val TYPE_CANONICAL_ROUTE_READY = "canonical_route_ready"
    private const val TYPE_SPARSE_MODE_SUGGESTION = "sparse_mode_suggestion"
    private const val TYPE_OPEN_HOLIDAY_MODE_LEGACY = "open_holiday_mode"
}
