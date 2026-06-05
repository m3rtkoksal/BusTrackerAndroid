package com.mikatechnology.BusTracker.services

import android.util.Log

internal object AnalyticsDebugStatus {
    private const val TAG = "BusTrackerAnalytics"

    fun isDebugModeEnabled(packageName: String): Boolean {
        return runCatching {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
            val value = getMethod.invoke(null, "debug.firebase.analytics.app", "") as String
            value == packageName
        }.getOrDefault(false)
    }

    fun logDebugViewHint(packageName: String, isDebugBuild: Boolean) {
        if (!isDebugBuild) return
        val enabled = isDebugModeEnabled(packageName)
        Log.i(TAG, "debugViewMode=$enabled")
        if (!enabled) {
            Log.w(
                TAG,
                "DebugView/Realtime icin once terminalde: " +
                    "adb shell setprop debug.firebase.analytics.app $packageName " +
                    "&& adb shell am force-stop $packageName"
            )
            Log.w(TAG, "Sonra uygulamayi ac. Upload logu: adb logcat -s FA:S FA-SVC:S BusTrackerAnalytics:I")
        }
    }
}
