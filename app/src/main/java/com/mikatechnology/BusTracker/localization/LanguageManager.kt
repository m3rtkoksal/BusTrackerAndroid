package com.mikatechnology.BusTracker.localization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "app_prefs"
    private const val LANGUAGE_KEY = "selected_language"

    private val _language = MutableStateFlow(AppLanguage.English)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = AppLanguage.fromCode(prefs.getString(LANGUAGE_KEY, null))
        _language.value = saved ?: defaultLanguage()
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        if (_language.value == language) return
        _language.value = language
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, language.code)
            .apply()
    }

    fun t(turkish: String, english: String): String {
        return if (_language.value == AppLanguage.Turkish) turkish else english
    }

    private fun defaultLanguage(): AppLanguage {
        val preferred = Locale.getDefault().language
        return if (preferred.startsWith("tr")) AppLanguage.Turkish else AppLanguage.English
    }
}
