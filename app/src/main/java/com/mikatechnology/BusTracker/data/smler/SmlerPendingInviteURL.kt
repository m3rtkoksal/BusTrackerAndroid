package com.mikatechnology.BusTracker.data.smler

import android.net.Uri
import android.util.Log

object SmlerPendingInviteURL {
    private const val TAG = "Smler"
    private var url: Uri? = null

    fun capture(uri: Uri) {
        if (!isInviteURL(uri)) return
        url = uri
        Log.d(TAG, "Launch URL kaydedildi: $uri")
    }

    fun consume(): Uri? {
        val captured = url
        url = null
        return captured
    }

    fun isInviteURL(uri: Uri): Boolean {
        if (uri.scheme?.lowercase() == "shuttlelive") return true
        return SmlerConfig.isSmlerLink(uri)
    }
}
