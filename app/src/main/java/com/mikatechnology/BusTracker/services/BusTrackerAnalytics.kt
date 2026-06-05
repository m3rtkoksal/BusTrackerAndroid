package com.mikatechnology.BusTracker.services

import android.os.Bundle
import android.util.Log
import com.mikatechnology.BusTracker.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

/** Firebase Analytics — PII/konum yok; yolcu + sürücü ürün olayları. */
object BusTrackerAnalytics {

    private const val TAG = "BusTrackerAnalytics"

    fun attendanceSelected(status: String) {
        log("attendance_selected", mapOf("status" to status))
    }

    fun pickupSaved() {
        log("pickup_saved")
    }

    fun holidayModeSaved() {
        log("holiday_mode_saved")
    }

    fun holidayModeEnded() {
        log("holiday_mode_ended")
    }

    fun sparseModePrompt(action: String) {
        log("sparse_mode_prompt", mapOf("action" to action))
    }

    fun tripStarted(durationHours: Double) {
        log("trip_started", mapOf("duration_hours" to durationHours))
    }

    fun logStartup(appInstanceId: String?) {
        Log.i(
            TAG,
            "Analytics hazir | collection=on | build=${if (BuildConfig.DEBUG) "debug" else "release"} | " +
                "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) | " +
                "instanceId=${appInstanceId ?: "pending"}"
        )
    }

    private fun log(name: String, params: Map<String, Any> = emptyMap()) {
        val analytics = Firebase.analytics
        analytics.setAnalyticsCollectionEnabled(true)
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putString(key, if (value) "1" else "0")
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(name, bundle)
        if (params.isEmpty()) {
            Log.i(TAG, "event=$name -> logEvent gonderildi")
        } else {
            Log.i(TAG, "event=$name params=$params -> logEvent gonderildi")
        }
    }
}
