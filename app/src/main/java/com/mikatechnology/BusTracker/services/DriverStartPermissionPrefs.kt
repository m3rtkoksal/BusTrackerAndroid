package com.mikatechnology.BusTracker.services

import android.content.Context

/** Sürücü servis başlat: sistem diyaloğu bir kez, sonra bottom sheet (iOS parity). */
object DriverStartPermissionPrefs {
    private const val PREFS_NAME = "bustracker_driver_start_permission_prefs"
    private const val KEY_FINE_LOCATION_REQUESTED = "fine_location_requested"
    private const val KEY_ACTIVITY_RECOGNITION_REQUESTED = "activity_recognition_requested"

    fun hasRequestedFineLocation(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FINE_LOCATION_REQUESTED, false)

    fun markFineLocationRequested(context: Context) {
        prefs(context).edit().putBoolean(KEY_FINE_LOCATION_REQUESTED, true).apply()
    }

    fun hasRequestedActivityRecognition(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACTIVITY_RECOGNITION_REQUESTED, false)

    fun markActivityRecognitionRequested(context: Context) {
        prefs(context).edit().putBoolean(KEY_ACTIVITY_RECOGNITION_REQUESTED, true).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
