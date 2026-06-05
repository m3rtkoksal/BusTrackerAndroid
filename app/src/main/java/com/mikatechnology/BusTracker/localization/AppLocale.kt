package com.mikatechnology.BusTracker.localization

import android.content.Context
import java.util.Locale

object AppLocale {
    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    fun apply(language: AppLanguage) {
        Locale.setDefault(Locale(language.code))
    }
}
