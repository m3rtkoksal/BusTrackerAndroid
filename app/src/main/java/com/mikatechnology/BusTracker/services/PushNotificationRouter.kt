package com.mikatechnology.BusTracker.services

import android.content.Intent
import com.google.firebase.messaging.RemoteMessage
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Bildirim tıklanınca role uygun ekrana yönlendirir. */
object PushNotificationRouter {
    private val _openPassengerMap = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openPassengerMap: SharedFlow<Unit> = _openPassengerMap.asSharedFlow()

    private val _openDriverMap = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openDriverMap: SharedFlow<Unit> = _openDriverMap.asSharedFlow()

    private val _openSparseModeSheet = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openSparseModeSheet: SharedFlow<Unit> = _openSparseModeSheet.asSharedFlow()

    @Volatile
    private var pendingOpenPassengerMap = false

    @Volatile
    private var pendingOpenDriverMap = false

    @Volatile
    private var pendingOpenSparseModeSheet = false

    fun handleIntent(intent: Intent?) {
        route(intent?.extras?.getString("type"))
    }

    fun handleRemoteMessage(message: RemoteMessage) {
        route(message.data["type"])
    }

    private fun route(type: String?) {
        val role = UserSessionRepository.profile.value?.role
        when (type) {
            TYPE_TRIP_STARTED, TYPE_PASSENGER_BOARDED -> {
                if (role == MemberRole.Passenger) signalOpenPassengerMap()
            }
            TYPE_CANONICAL_ROUTE_READY -> when (role) {
                MemberRole.Driver -> signalOpenDriverMap()
                MemberRole.Passenger -> signalOpenPassengerMap()
                null -> Unit
            }
            TYPE_TRIP_ENDED -> Unit
            SparseModeSuggestion.INTENT_TYPE,
            TYPE_SPARSE_MODE_SUGGESTION,
            TYPE_OPEN_HOLIDAY_MODE_LEGACY -> {
                if (role == MemberRole.Passenger) signalOpenSparseModeSheet()
            }
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

    fun consumePendingOpenDriverMap(): Boolean {
        if (!pendingOpenDriverMap) return false
        pendingOpenDriverMap = false
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

    private fun signalOpenDriverMap() {
        pendingOpenDriverMap = true
        _openDriverMap.tryEmit(Unit)
    }

    private const val TYPE_TRIP_STARTED = "trip_started"
    private const val TYPE_TRIP_ENDED = "trip_ended"
    private const val TYPE_PASSENGER_BOARDED = "passenger_boarded"
    private const val TYPE_CANONICAL_ROUTE_READY = "canonical_route_ready"
    private const val TYPE_SPARSE_MODE_SUGGESTION = "sparse_mode_suggestion"
    private const val TYPE_OPEN_HOLIDAY_MODE_LEGACY = "open_holiday_mode"
}
