package com.mikatechnology.BusTracker.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object CopyServiceCode {
    fun copy(context: Context, code: String): Boolean {
        val text = code.trim().uppercase()
        if (text.isEmpty()) return false
        val appContext = context.applicationContext
        return try {
            val clipboard = appContext.getSystemService(ClipboardManager::class.java) ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText("Servis Kodu", text))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun showResult(context: Context, copied: Boolean) {
        val message = if (copied) "Servis kodu kopyalandı." else "Servis kodu bulunamadı."
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
