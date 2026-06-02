package com.mikatechnology.BusTracker.services

import android.content.Context
import android.location.Geocoder
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.localization.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

data class PassengerWeatherCardModel(
    val placeName: String,
    val temperatureC: Int,
    val advice: String,
    val emoji: String
) {
    val contextLine: String
        get() = L10n.weatherContext(placeName, temperatureC)
}

object PassengerWeatherService {
    private const val OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast"
    private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse"
    private const val CACHE_TTL_MS = 3_600_000L
    private const val CACHE_PREFIX = "passengerWeather."
    private const val PREFS_NAME = "passenger_weather_cache"
    private val NEIGHBORHOOD_ADDRESS_KEYS = listOf(
        "neighbourhood",
        "suburb",
        "quarter",
        "hamlet",
        "residential",
        "city_district"
    )
    private const val COLD_TEMP_C = 6.0
    private const val HOT_TEMP_C = 31.0
    private const val WET_MM = 0.15

    private data class WeatherCachePayload(
        val placeName: String,
        val tempC: Double,
        val precipitation: Double,
        val rain: Double,
        val fetchedAt: Long
    )

    fun cachedModel(
        context: Context,
        latitude: Double,
        longitude: Double
    ): PassengerWeatherCardModel? {
        if (!isValidCoordinate(latitude, longitude)) return null
        val entry = cachedEntry(context, latitude, longitude) ?: return null
        return modelFrom(entry)
    }

    suspend fun load(
        context: Context,
        latitude: Double,
        longitude: Double
    ): PassengerWeatherCardModel? {
        if (!isValidCoordinate(latitude, longitude)) return null

        cachedEntry(context, latitude, longitude)?.let { return modelFrom(it) }

        val weather = fetchWeather(latitude, longitude) ?: return null
        val placeName = resolvePlaceName(context, latitude, longitude)
        val entry = WeatherCachePayload(
            placeName = placeName,
            tempC = weather.tempC,
            precipitation = weather.precipitation,
            rain = weather.rain,
            fetchedAt = System.currentTimeMillis()
        )
        saveCache(context, latitude, longitude, entry)
        return modelFrom(entry)
    }

    private fun cacheStorageKey(latitude: Double, longitude: Double): String {
        return CACHE_PREFIX + String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
    }

    private fun cachedEntry(
        context: Context,
        latitude: Double,
        longitude: Double
    ): WeatherCachePayload? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(cacheStorageKey(latitude, longitude), null) ?: return null
        return try {
            val json = JSONObject(raw)
            val fetchedAt = json.getLong("fetchedAt")
            if (System.currentTimeMillis() - fetchedAt >= CACHE_TTL_MS) return null
            WeatherCachePayload(
                placeName = json.getString("placeName"),
                tempC = json.getDouble("tempC"),
                precipitation = json.getDouble("precipitation"),
                rain = json.getDouble("rain"),
                fetchedAt = fetchedAt
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun saveCache(
        context: Context,
        latitude: Double,
        longitude: Double,
        entry: WeatherCachePayload
    ) {
        val json = JSONObject()
            .put("placeName", entry.placeName)
            .put("tempC", entry.tempC)
            .put("precipitation", entry.precipitation)
            .put("rain", entry.rain)
            .put("fetchedAt", entry.fetchedAt)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(cacheStorageKey(latitude, longitude), json.toString())
            .apply()
    }

    private fun modelFrom(entry: WeatherCachePayload): PassengerWeatherCardModel {
        val (advice, emoji) = clothingAdvice(
            tempC = entry.tempC,
            precipitation = entry.precipitation,
            rain = entry.rain
        )
        return PassengerWeatherCardModel(
            placeName = entry.placeName,
            temperatureC = entry.tempC.roundToInt(),
            advice = advice,
            emoji = emoji
        )
    }

    private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        if (kotlin.math.abs(latitude) > 90 || kotlin.math.abs(longitude) > 180) return false
        if (kotlin.math.abs(latitude) <= 0.01 && kotlin.math.abs(longitude) <= 0.01) return false
        return true
    }

    private data class WeatherReading(
        val tempC: Double,
        val precipitation: Double,
        val rain: Double
    )

    private suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherReading? =
        withContext(Dispatchers.IO) {
            val url = URL(
                "$OPEN_METEO_URL?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,precipitation,rain&timezone=auto"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val body = connection.inputStream.bufferedReader().readText()
                val current = JSONObject(body).optJSONObject("current") ?: return@withContext null
                if (!current.has("temperature_2m")) return@withContext null
                WeatherReading(
                    tempC = current.getDouble("temperature_2m"),
                    precipitation = current.optDouble("precipitation", 0.0),
                    rain = current.optDouble("rain", 0.0)
                )
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }

    private suspend fun resolvePlaceName(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String =
        withContext(Dispatchers.IO) {
            fetchNeighborhoodFromNominatim(latitude, longitude)
                ?: fetchNeighborhoodFromDeviceGeocoder(context, latitude, longitude)
                ?: L10n.pickupPlaceFallback
        }

    private fun fetchNeighborhoodFromNominatim(latitude: Double, longitude: Double): String? {
        val languageCode = LanguageManager.language.value.code
        val url = URL(
            "$NOMINATIM_URL?lat=$latitude&lon=$longitude&format=json" +
                "&accept-language=$languageCode&zoom=17"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "BusTracker/1.3 (passenger-clothing-advice)")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val address = JSONObject(connection.inputStream.bufferedReader().readText())
                .optJSONObject("address") ?: return null
            for (key in NEIGHBORHOOD_ADDRESS_KEYS) {
                address.optString(key).takeIf { it.isNotBlank() }?.let { return it.trim() }
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchNeighborhoodFromDeviceGeocoder(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            @Suppress("DEPRECATION")
            val locale = Locale(LanguageManager.language.value.code)
            val geocoder = Geocoder(context, locale)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.subLocality?.takeIf { it.isNotBlank() }?.trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun clothingAdvice(
        tempC: Double,
        precipitation: Double,
        rain: Double
    ): Pair<String, String> {
        val wet = precipitation >= WET_MM || rain >= WET_MM
        return when {
            wet -> L10n.adviceRain to "🌧️"
            tempC <= COLD_TEMP_C -> L10n.adviceColdHat to "🧣"
            tempC >= HOT_TEMP_C -> L10n.adviceVeryHot to "☀️"
            tempC >= 22 -> L10n.adviceHot to "☀️"
            tempC >= 12 -> L10n.adviceCool to "🧥"
            else -> L10n.adviceCold to "🧣"
        }
    }
}
