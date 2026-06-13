package com.mikatechnology.BusTracker.data.smler

import com.mikatechnology.BusTracker.BuildConfig
import com.mikatechnology.BusTracker.localization.L10n

object SmlerConfig {
    const val LINK_DOMAIN = "shuttlelive.smler.io"
    const val RESOLVE_API_BASE = "https://smler.in/api/v1"
    const val CREATE_API_BASE = "https://smler.in/api/v1"

    val inviteOGTitle: String get() = L10n.smlerOGTitle
    val inviteOGDescription: String get() = L10n.smlerOGDescription
    const val INVITE_OG_IMAGE_URL = "https://mika.technology/shuttle-live-og.png"

    fun destinationURL(serviceCode: String): String {
        val code = normalizedCode(serviceCode)
        return "shuttlelive://passenger/join?code=$code"
    }

    fun shortLinkURL(shortCode: String): String? {
        val code = normalizedCode(shortCode)
        if (code.length < 4) return null
        return "https://$LINK_DOMAIN/$code"
    }

    fun normalizedCode(raw: String): String =
        raw.trim().uppercase()

    fun isSmlerLink(url: String): Boolean {
        val host = runCatching { android.net.Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
        return host == LINK_DOMAIN || host.endsWith(".$LINK_DOMAIN")
    }

    fun isSmlerLink(uri: android.net.Uri): Boolean =
        uri.host?.let { host ->
            val lower = host.lowercase()
            lower == LINK_DOMAIN || lower.endsWith(".$LINK_DOMAIN")
        } == true

    val apiKey: String?
        get() {
            val trimmed = BuildConfig.SMLER_API_KEY.trim()
            return trimmed.takeIf { it.isNotEmpty() }
        }
}
