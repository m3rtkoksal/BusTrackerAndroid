package com.mikatechnology.BusTracker.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.mikatechnology.BusTracker.localization.L10n

object CopyServiceCode {
    fun copy(context: Context, code: String): Boolean {
        val text = code.trim().uppercase()
        if (text.isEmpty()) return false
        val appContext = context.applicationContext
        return try {
            val clipboard = appContext.getSystemService(ClipboardManager::class.java) ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText(L10n.settingsServiceCode, text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun showResult(context: Context, copied: Boolean) {
        val message = if (copied) L10n.serviceCodeCopied else L10n.serviceCodeNotFound
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
