package com.mikatechnology.BusTracker

import android.app.Application
import android.content.Context
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.analytics
import com.mikatechnology.BusTracker.BuildConfig
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.localization.AppLanguage
import com.mikatechnology.BusTracker.localization.AppLocale
import com.mikatechnology.BusTracker.services.AnalyticsDebugStatus
import com.mikatechnology.BusTracker.services.BusTrackerAnalytics
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.services.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.mikatechnology.BusTracker.localization.LanguageManager
import java.util.Locale

class BusTrackerApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("app_prefs", MODE_PRIVATE)
        val saved = AppLanguage.fromCode(prefs.getString("selected_language", null))
        val language = saved ?: run {
            val preferred = Locale.getDefault().language
            if (preferred.startsWith("tr")) AppLanguage.Turkish else AppLanguage.English
        }
        super.attachBaseContext(AppLocale.wrap(base, language))
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
        appScope.launch {
            val instanceId = runCatching {
                Firebase.analytics.appInstanceId.await()
            }.getOrNull()
            BusTrackerAnalytics.logStartup(instanceId)
            AnalyticsDebugStatus.logDebugViewHint(packageName, BuildConfig.DEBUG)
        }
        LanguageManager.initialize(this)
        AuthRepository.ensureConfigured()
        NotificationService.createNotificationChannels(this)
        LocationTracker.initialize(this)
        if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) {}
        }
    }
}
