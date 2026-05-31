package com.mikatechnology.BusTracker.services

import android.content.Context
import android.location.Geocoder
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
    /** Push bildirimiyle aynı tarz giyim önerisi metni. */
    val advice: String,
    val emoji: String
) {
    val contextLine: String
        get() = "Bugün $placeName · $temperatureC°"
}

object PassengerWeatherService {
    private const val OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast"
    private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse"
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

    suspend fun load(
        context: Context,
        latitude: Double,
        longitude: Double
    ): PassengerWeatherCardModel? {
        if (!isValidCoordinate(latitude, longitude)) return null
        val weather = fetchWeather(latitude, longitude) ?: return null
        val placeName = resolvePlaceName(context, latitude, longitude)
        val (advice, emoji) = clothingAdvice(
            tempC = weather.tempC,
            precipitation = weather.precipitation,
            rain = weather.rain
        )
        return PassengerWeatherCardModel(
            placeName = placeName,
            temperatureC = weather.tempC.roundToInt(),
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

    /** Mahalle — iOS ile aynı OSM kaynağı; şehir/ilçe yok. */
    private suspend fun resolvePlaceName(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String =
        withContext(Dispatchers.IO) {
            fetchNeighborhoodFromNominatim(latitude, longitude)
                ?: fetchNeighborhoodFromDeviceGeocoder(context, latitude, longitude)
                ?: "Biniş noktan"
        }

    private fun fetchNeighborhoodFromNominatim(latitude: Double, longitude: Double): String? {
        val url = URL(
            "$NOMINATIM_URL?lat=$latitude&lon=$longitude&format=json" +
                "&accept-language=tr&zoom=17"
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
            val geocoder = Geocoder(context, Locale("tr", "TR"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.subLocality?.takeIf { it.isNotBlank() }?.trim()
        } catch (_: Exception) {
            null
        }
    }

    /** functions/weather.js ile aynı eşikler; ara sıcaklıklar için ek öneri. */
    private fun clothingAdvice(
        tempC: Double,
        precipitation: Double,
        rain: Double
    ): Pair<String, String> {
        val wet = precipitation >= WET_MM || rain >= WET_MM
        return when {
            wet -> "Yağmur var — şemsiyeni kap." to "🌧️"
            tempC <= COLD_TEMP_C -> "Hava soğuk — bere takmadan çıkma." to "🧣"
            tempC >= HOT_TEMP_C -> "Hava cehennem gibi — şapka tak, su al." to "☀️"
            tempC >= 22 -> "Hava sıcak — şapka tak, su al." to "☀️"
            tempC >= 12 -> "Hava serin — ince mont veya hırka al." to "🧥"
            else -> "Hava soğuk — kalın giyin." to "🧣"
        }
    }
}
