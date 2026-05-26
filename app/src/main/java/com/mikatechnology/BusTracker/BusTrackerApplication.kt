package com.mikatechnology.BusTracker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.android.gms.maps.MapsInitializer
import com.mikatechnology.BusTracker.BuildConfig
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.services.LocationTracker

class BusTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AuthRepository.ensureConfigured()
        LocationTracker.initialize(this)
        if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) {}
        }
    }
}
