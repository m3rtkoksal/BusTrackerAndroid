package com.mikatechnology.BusTracker.localization

enum class AppLanguage(val code: String, val displayName: String) {
    Turkish("tr", "Türkçe"),
    English("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage? {
            return entries.firstOrNull { it.code == code }
        }
    }
}
