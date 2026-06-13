package com.mikatechnology.BusTracker.data.smler

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.mikatechnology.BusTracker.localization.L10n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class SmlerShareOutcome {
    data class Success(val message: String, val shortURL: String) : SmlerShareOutcome()
    data class Failure(val error: String) : SmlerShareOutcome()
}

object SmlerDeepLinkService {
    private const val TAG = "Smler"
    private const val PREFS_NAME = "smler"
    private const val DEFERRED_HANDLED_KEY = "smler_deferred_handled_v2"
    private const val URL_CACHE_PREFIX = "smler_cached_url_"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    suspend fun prepareShare(serviceCode: String): SmlerShareOutcome {
        val code = SmlerConfig.normalizedCode(serviceCode)
        log("prepareShare başladı serviceCode=$code")
        if (code.length < 4) {
            return SmlerShareOutcome.Failure(L10n.invalidServiceCode)
        }
        if (SmlerConfig.apiKey == null) {
            return SmlerShareOutcome.Failure(L10n.smlerAPIKeyMissingInfo)
        }

        return when (val result = createInviteDeepLink(code)) {
            is CreateLinkResult.Success -> {
                val message = buildString {
                    appendLine(L10n.smlerShareTitle)
                    appendLine()
                    append(L10n.smlerShareBody(code, result.url))
                }
                SmlerShareOutcome.Success(message = message, shortURL = result.url)
            }
            is CreateLinkResult.Failure -> SmlerShareOutcome.Failure(result.error)
        }
    }

    suspend fun ensureInviteLink(serviceCode: String): String? {
        val code = SmlerConfig.normalizedCode(serviceCode)
        if (code.length < 4 || SmlerConfig.apiKey == null) return null
        return when (val result = createInviteDeepLink(code)) {
            is CreateLinkResult.Success -> result.url
            is CreateLinkResult.Failure -> null
        }
    }

    suspend fun serviceCodeFrom(url: Uri): String? {
        if (!SmlerConfig.isSmlerLink(url) && url.scheme?.lowercase() != "shuttlelive") {
            return serviceCodeFromDestination(url)
        }
        return resolveServiceCodeFromSmlerURL(url)
    }

    suspend fun serviceCodeFromDeferredInstall(): String? {
        if (prefs().getBoolean(DEFERRED_HANDLED_KEY, false)) return null

        val retryDelaysMs = listOf(0L, 800L, 2_000L, 4_000L)
        for (delayMs in retryDelaysMs) {
            if (delayMs > 0) delay(delayMs)
            readClipboardInviteURL()?.let { clipboardUrl ->
                log("Deferred pano eşleşmesi: $clipboardUrl")
                resolveServiceCodeFromSmlerURL(clipboardUrl, triggerWebhook = true)?.let { code ->
                    markDeferredInstallHandled()
                    return code
                }
            }
        }

        val probabilisticCode = probabilisticServiceCode()
        markDeferredInstallHandled()
        return probabilisticCode
    }

    private fun markDeferredInstallHandled() {
        prefs().edit().putBoolean(DEFERRED_HANDLED_KEY, true).apply()
    }

    private sealed class CreateLinkResult {
        data class Success(val url: String) : CreateLinkResult()
        data class Failure(val error: String) : CreateLinkResult()
    }

    private suspend fun createInviteDeepLink(serviceCode: String): CreateLinkResult = withContext(Dispatchers.IO) {
        val code = SmlerConfig.normalizedCode(serviceCode)
        cachedURL(code)?.let { cached ->
            if (verifyShortLinkExists(cached)) {
                log("Önbellekten link: $cached")
                return@withContext CreateLinkResult.Success(cached)
            }
            log("Önbellekteki link geçersiz, yeniden oluşturulacak: $cached")
            clearCache(code)
        }

        val apiKey = SmlerConfig.apiKey
            ?: return@withContext CreateLinkResult.Failure(L10n.smlerAPIKeyMissing)

        val endpoint = "${SmlerConfig.CREATE_API_BASE}/short"
        val body = JSONObject().apply {
            put("url", SmlerConfig.destinationURL(code))
            put("shortCode", code)
            put("domain", SmlerConfig.LINK_DOMAIN)
            put("maxLength", maxOf(code.length, 6))
            put("forever", true)
            put("isDeferredLink", true)
            put("ogTitle", SmlerConfig.inviteOGTitle)
            put("ogDescription", SmlerConfig.inviteOGDescription)
            put("ogImage", SmlerConfig.INVITE_OG_IMAGE_URL)
        }

        try {
            val connection = openConnection(endpoint, "POST", apiKey)
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            val status = connection.responseCode
            val responseText = readResponseBody(connection)

            if (status !in 200..299) {
                val detail = apiErrorMessage(responseText) ?: shortAPIErrorHint(status)
                if (status == 400 || status == 409) {
                    val bodyText = responseText.lowercase()
                    if (bodyText.contains("already exists") || bodyText.contains("unable to create short url")) {
                        fetchExistingSecureShortUrl(code)?.let { existing ->
                            log("Smler'den mevcut link alındı: $existing")
                            cacheURL(code, existing)
                            return@withContext CreateLinkResult.Success(existing)
                        }
                    }
                }
                return@withContext CreateLinkResult.Failure(L10n.smlerLinkFailed(status, detail))
            }

            val shortUrl = parseShortURL(responseText) ?: fetchExistingSecureShortUrl(code)
            if (shortUrl != null) {
                cacheURL(code, shortUrl)
                return@withContext CreateLinkResult.Success(shortUrl)
            }
            CreateLinkResult.Failure(L10n.smlerShortLinkMissing)
        } catch (error: Exception) {
            CreateLinkResult.Failure(L10n.connectionErrorDetail(error.message ?: error.toString()))
        }
    }

    private suspend fun fetchExistingSecureShortUrl(shortCode: String): String? {
        val query = buildString {
            append("${SmlerConfig.RESOLVE_API_BASE}/short?")
            append("short=${URLEncoder.encode(shortCode, Charsets.UTF_8.name())}")
            append("&domain=${URLEncoder.encode(SmlerConfig.LINK_DOMAIN, Charsets.UTF_8.name())}")
        }
        return try {
            val connection = openConnection(query, "GET")
            if (connection.responseCode !in 200..299) return null
            parseShortURL(readResponseBody(connection))
        } catch (_: Exception) {
            null
        }
    }

    private fun verifyShortLinkExists(url: String): Boolean {
        return try {
            val connection = openConnection(url, "HEAD")
            connection.instanceFollowRedirects = false
            val status = connection.responseCode
            val location = connection.getHeaderField("Location").orEmpty()
            when {
                location.contains("code-not-found", ignoreCase = true) -> false
                status in 200..299 -> true
                status in 300..399 -> !location.contains("code-not-found", ignoreCase = true)
                else -> false
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun openConnection(urlString: String, method: String, apiKey: String? = null): HttpURLConnection =
        (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 25_000
            readTimeout = 25_000
            setRequestProperty("Content-Type", "application/json")
            apiKey?.let { setRequestProperty("x-code", it) }
            doInput = true
            if (method == "POST") doOutput = true
        }

    private fun readResponseBody(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
    }

    private fun apiErrorMessage(responseText: String): String? = runCatching {
        JSONObject(responseText).optString("message").takeIf { it.isNotBlank() }
            ?: JSONObject(responseText).optString("error").takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun shortAPIErrorHint(status: Int): String =
        if (status == 404) L10n.apiURLNotFound else L10n.checkLogcat

    private suspend fun resolveServiceCodeFromSmlerURL(
        url: Uri,
        triggerWebhook: Boolean = false
    ): String? {
        if (url.scheme?.lowercase() == "shuttlelive") {
            serviceCodeFromDestination(url)?.let { return it }
        }

        val pathParams = extractPathParams(url)
        if (pathParams.shortCode.isEmpty()) return null

        fetchResolvedDestination(
            shortCode = pathParams.shortCode,
            dltHeader = pathParams.dltHeader,
            triggerWebhook = triggerWebhook
        )?.let { destination ->
            serviceCodeFromDestination(destination)?.let { return it }
        }

        return if (pathParams.shortCode.length >= 4) pathParams.shortCode else null
    }

    private fun readClipboardInviteURL(): Uri? {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = clipboard?.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val text = clip.getItemAt(0).coerceToText(appContext)?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return null

        parseURLLikeString(text)?.let { uri ->
            if (uri.scheme?.lowercase() == "shuttlelive") return uri
        }

        listOf(
            "https://${SmlerConfig.LINK_DOMAIN}",
            "http://${SmlerConfig.LINK_DOMAIN}",
            SmlerConfig.LINK_DOMAIN
        ).forEach { pattern ->
            if (matchesDeepLinkPattern(text, pattern)) {
                return parseURLLikeString(text)
            }
        }
        return null
    }

    private suspend fun probabilisticServiceCode(): String? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("device", Build.MODEL ?: "Android")
            put("os", "Android ${Build.VERSION.RELEASE}")
            put("domain", SmlerConfig.LINK_DOMAIN)
        }

        try {
            val connection = openConnection("https://smler.in/api/v2/track/probablistic", "POST")
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            if (connection.responseCode !in 200..299) return@withContext null
            val json = JSONObject(readResponseBody(connection))
            if (!json.optBoolean("matched") || json.optDouble("score", 0.0) <= 0.65) return@withContext null

            json.optJSONObject("shortUrl")?.optString("originalUrl")?.takeIf { it.isNotBlank() }?.let { original ->
                serviceCodeFromDestination(Uri.parse(original))?.let { return@withContext it }
            }

            json.optJSONObject("pathParams")?.optString("shortCode")?.let { shortCode ->
                val normalized = SmlerConfig.normalizedCode(shortCode)
                if (normalized.length >= 4) return@withContext normalized
            }
        } catch (_: Exception) {
        }
        null
    }

    private data class SmlerPathParams(val shortCode: String, val dltHeader: String?)

    private fun extractPathParams(url: Uri): SmlerPathParams {
        val segments = url.pathSegments.filter { it.isNotBlank() }
        if (segments.size >= 2) {
            return SmlerPathParams(
                shortCode = SmlerConfig.normalizedCode(segments[1]),
                dltHeader = segments[0]
            )
        }
        segments.firstOrNull()?.let { first ->
            return SmlerPathParams(
                shortCode = SmlerConfig.normalizedCode(first),
                dltHeader = null
            )
        }
        return SmlerPathParams(shortCode = "", dltHeader = null)
    }

    private suspend fun fetchResolvedDestination(
        shortCode: String,
        dltHeader: String?,
        triggerWebhook: Boolean = false
    ): Uri? = withContext(Dispatchers.IO) {
        val query = buildString {
            append("${SmlerConfig.RESOLVE_API_BASE}/short?")
            append("short=${URLEncoder.encode(shortCode, Charsets.UTF_8.name())}")
            append("&domain=${URLEncoder.encode(SmlerConfig.LINK_DOMAIN, Charsets.UTF_8.name())}")
            if (!dltHeader.isNullOrBlank()) {
                append("&dltHeader=${URLEncoder.encode(dltHeader, Charsets.UTF_8.name())}")
            }
            if (triggerWebhook) append("&triggerWebhook=true")
        }
        try {
            val connection = openConnection(query, "GET")
            if (connection.responseCode !in 200..299) return@withContext null
            parseDestinationURL(readResponseBody(connection))?.let { Uri.parse(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun serviceCodeFromDestination(url: Uri): String? {
        url.getQueryParameter("code")?.let { code ->
            val normalized = SmlerConfig.normalizedCode(code)
            if (normalized.length >= 4) return normalized
        }
        val parts = url.pathSegments
        val joinIndex = parts.indexOfFirst { it.equals("join", ignoreCase = true) }
        if (joinIndex >= 0 && joinIndex + 1 < parts.size) {
            val normalized = SmlerConfig.normalizedCode(parts[joinIndex + 1])
            if (normalized.length >= 4) return normalized
        }
        return null
    }

    private fun parseShortURL(responseText: String): String? {
        val json = runCatching { JSONObject(responseText) }.getOrNull() ?: return null
        val link = json.optJSONObject("link")
        val dataObj = json.optJSONObject("data")
        listOf(
            json.optString("secureShortUrl"),
            json.optString("shortUrl"),
            json.optString("shortURL"),
            json.optString("shortLink"),
            json.optString("short_url"),
            link?.optString("shortUrl"),
            link?.optString("shortURL"),
            link?.optString("url"),
            dataObj?.optString("shortUrl"),
            dataObj?.optString("url")
        ).forEach { candidate ->
            val trimmed = candidate?.trim().orEmpty()
            if (trimmed.isNotEmpty() &&
                (SmlerConfig.isSmlerLink(trimmed) || trimmed.contains(SmlerConfig.LINK_DOMAIN))
            ) {
                return trimmed
            }
        }
        return null
    }

    private fun parseDestinationURL(responseText: String): String? {
        val json = runCatching { JSONObject(responseText) }.getOrNull() ?: return null
        return listOf(
            json.optString("originalUrl"),
            json.optString("originalURL"),
            json.optJSONObject("link")?.optString("url"),
            json.optJSONObject("data")?.optString("url"),
            json.optString("url")
        ).firstOrNull { !it.isNullOrBlank() }
    }

    private fun cachedURL(code: String): String? =
        prefs().getString(URL_CACHE_PREFIX + code, null)

    private fun cacheURL(code: String, url: String) {
        prefs().edit().putString(URL_CACHE_PREFIX + code, url).apply()
    }

    private fun clearCache(code: String) {
        prefs().edit().remove(URL_CACHE_PREFIX + code).apply()
    }

    private fun matchesDeepLinkPattern(clipboard: String, pattern: String): Boolean {
        val trimmedPattern = pattern.trim()
        if (trimmedPattern.isEmpty()) return false

        val normalizedClipboard = normalizeURLLikeString(clipboard)
        val normalizedPattern = normalizeURLLikeString(trimmedPattern)
        if (normalizedClipboard == normalizedPattern || normalizedClipboard.startsWith(normalizedPattern)) {
            return true
        }

        val clipboardURI = parseURLLikeString(clipboard) ?: return false
        val patternURI = parseURLLikeString(trimmedPattern) ?: return false

        fun stripWWW(host: String): String =
            if (host.lowercase().startsWith("www.")) host.drop(4).lowercase() else host.lowercase()

        val clipboardHost = stripWWW(clipboardURI.host.orEmpty())
        val patternHost = stripWWW(patternURI.host.orEmpty())
        if (clipboardHost.isEmpty() || patternHost.isEmpty()) return false

        val hostMatches = clipboardHost == patternHost || clipboardHost.endsWith(".$patternHost")
        if (!hostMatches) return false

        val patternPath = patternURI.path.orEmpty()
        if (patternPath.isEmpty() || patternPath == "/") return true
        val clipboardPath = clipboardURI.path?.ifEmpty { "/" } ?: "/"
        return clipboardPath.startsWith(patternPath)
    }

    private fun normalizeURLLikeString(value: String): String {
        var normalized = value.trim()
        when {
            normalized.lowercase().startsWith("https://") ->
                normalized = normalized.drop("https://".length)
            normalized.lowercase().startsWith("http://") ->
                normalized = normalized.drop("http://".length)
        }
        return normalized
    }

    private fun parseURLLikeString(value: String): Uri? {
        val trimmed = value.trim()
        val direct = Uri.parse(trimmed)
        if (direct.host != null) return direct
        return Uri.parse("https://$trimmed")
    }
}
