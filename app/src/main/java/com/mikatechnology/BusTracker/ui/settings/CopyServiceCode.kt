package com.mikatechnology.BusTracker.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.mikatechnology.BusTracker.localization.L10n

object CopyServiceCode {
    fun copy(context: Context, code: String): Boolean {
        return copyPlainText(context, code.trim().uppercase(), L10n.settingsServiceCode)
    }

    fun copyPlainText(
        context: Context,
        text: String,
        label: String = L10n.settingsServiceCode
    ): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val appContext = context.applicationContext
        return try {
            val clipboard = appContext.getSystemService(ClipboardManager::class.java) ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText(label, trimmed))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun showResult(context: Context, copied: Boolean) {
        val message = if (copied) L10n.serviceCodeCopied else L10n.serviceCodeNotFound
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    fun showPlainCopyResult(context: Context, copied: Boolean, successMessage: String) {
        Toast.makeText(
            context.applicationContext,
            if (copied) successMessage else L10n.serviceCodeNotFound,
            Toast.LENGTH_SHORT
        ).show()
    }
}
