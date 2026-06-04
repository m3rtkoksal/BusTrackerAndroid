package com.mikatechnology.BusTracker.data.model

import android.location.Location
import com.google.firebase.Timestamp
import java.util.Date

/** Sürücü hız örnekleri — bindi analizi (duraklama penceresi). */
object TripTelemetry {
    const val SAMPLE_WINDOW_MS = 900_000L
    const val MAX_STORED_SAMPLES = 48

    data class Sample(
        val speedMps: Double,
        val latitude: Double,
        val longitude: Double,
        val sampledAt: Date
    )

    fun speedMps(location: Location, previous: Location?): Double {
        if (location.hasSpeed() && location.speed >= 0f) {
            return location.speed.toDouble()
        }
        if (previous == null) return 0.0
        val dtSeconds = (location.time - previous.time) / 1000.0
        if (dtSeconds <= 0) return 0.0
        val results = FloatArray(1)
        Location.distanceBetween(
            previous.latitude,
            previous.longitude,
            location.latitude,
            location.longitude,
            results
        )
        return results[0] / dtSeconds
    }

    fun firestorePayload(sample: Sample): Map<String, Any> =
        mapOf(
            "speedMps" to sample.speedMps,
            "latitude" to sample.latitude,
            "longitude" to sample.longitude,
            "sampledAt" to Timestamp(sample.sampledAt)
        )

    fun samplesFrom(data: Map<String, Any>?): List<Sample> {
        val raw = data?.get("samples") as? List<*> ?: return emptyList()
        val cutoff = Date(System.currentTimeMillis() - SAMPLE_WINDOW_MS)
        return raw.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val speed = (map["speedMps"] as? Number)?.toDouble() ?: return@mapNotNull null
            val lat = (map["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
            val lng = (map["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
            val at = (map["sampledAt"] as? Timestamp)?.toDate() ?: return@mapNotNull null
            if (at.before(cutoff)) return@mapNotNull null
            Sample(speedMps = speed, latitude = lat, longitude = lng, sampledAt = at)
        }.sortedBy { it.sampledAt }
    }
}
