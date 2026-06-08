package com.mikatechnology.BusTracker.services

import java.util.Date
import java.util.concurrent.TimeUnit

/** Yolcu motion verisiyle seferin bitip bitmediğini değerlendirir. */
object TripMotionAutoStop {
    private val MIN_TRIP_DURATION_MS = TimeUnit.MINUTES.toMillis(15)
    private val MIN_SECONDS_AFTER_VEHICLE_EXIT_MS = TimeUnit.SECONDS.toMillis(45)
    private val MIN_WALKING_MS = TimeUnit.SECONDS.toMillis(90)
    private val STATIONARY_FALLBACK_EXTRA_MS = TimeUnit.SECONDS.toMillis(30)

    data class PassengerMotionSnapshot(
        val memberID: String,
        val inVehicle: Boolean,
        val currentActivity: String,
        val hasBeenInVehicle: Boolean,
        val lastVehicleExitAt: Date?,
        val walkingSince: Date?,
        val hadPickupReach: Boolean
    )

    fun shouldAutoStopTrip(
        tripStartedAt: Date,
        comingMemberIDs: Set<String>,
        passengers: List<PassengerMotionSnapshot>,
        now: Date = Date()
    ): Boolean {
        if (now.time - tripStartedAt.time < MIN_TRIP_DURATION_MS) return false
        if (comingMemberIDs.isEmpty()) return false

        val trackableIDs = comingMemberIDs.filter { memberID ->
            val snapshot = passengers.firstOrNull { it.memberID == memberID } ?: return@filter false
            snapshot.hadPickupReach || snapshot.hasBeenInVehicle
        }
        if (trackableIDs.isEmpty()) return false

        val alightedCount = trackableIDs.count { memberID ->
            val snapshot = passengers.firstOrNull { it.memberID == memberID } ?: return@count false
            isAlighted(snapshot, now)
        }
        return alightedCount == trackableIDs.size
    }

    fun isAlighted(snapshot: PassengerMotionSnapshot, now: Date): Boolean {
        if (!snapshot.hadPickupReach && !snapshot.hasBeenInVehicle) return false
        if (snapshot.inVehicle) return false
        val exitAt = snapshot.lastVehicleExitAt ?: return false
        if (now.time - exitAt.time < MIN_SECONDS_AFTER_VEHICLE_EXIT_MS) return false

        if (snapshot.currentActivity == "walking") {
            val walkingSince = snapshot.walkingSince ?: return false
            return now.time - walkingSince.time >= MIN_WALKING_MS
        }

        if (snapshot.currentActivity == "stationary") {
            return now.time - exitAt.time >= MIN_WALKING_MS + STATIONARY_FALLBACK_EXTRA_MS
        }

        return false
    }
}
