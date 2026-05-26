package com.mikatechnology.BusTracker.data.model

import android.location.Location
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

object MapDefaults {
    /** Sancaktepe Atatürk mahallesi, 34785 */
    val homeLatLng = LatLng(41.013298, 29.224813)

    val homeCameraPosition: CameraPosition
        get() = CameraPosition.fromLatLngZoom(homeLatLng, 14f)

    val homeLocation: Location
        get() = Location("simulator").apply {
            latitude = homeLatLng.latitude
            longitude = homeLatLng.longitude
        }

    fun isNearHome(latitude: Double, longitude: Double): Boolean {
        return latitude in 36.0..42.5 && longitude in 25.0..45.0
    }
}
