package com.mikatechnology.BusTracker.ui.map

import android.location.Location
import com.mikatechnology.BusTracker.data.model.DriverLocation
import java.util.Date

/**
 * Firestore konumu (aktif servis) yoksa sürücü haritasında cihaz konumunu göster.
 */
fun resolveDriverMapLocation(
    firestoreLocation: DriverLocation?,
    deviceLocation: Location?,
    driverName: String
): DriverLocation? {
    firestoreLocation?.let { return it }

    val location = deviceLocation ?: return null
    return DriverLocation(
        latitude = location.latitude,
        longitude = location.longitude,
        updatedAt = Date(location.time),
        isActive = false,
        driverName = driverName
    )
}
