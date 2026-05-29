package com.mikatechnology.BusTracker.data.repository

import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.mikatechnology.BusTracker.auth.GoogleSignInHelper
import com.mikatechnology.BusTracker.auth.GoogleSignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthError(message: String) : Exception(message) {
    class FirebaseNotReady : AuthError("Firebase henüz hazır değil. Lütfen tekrar deneyin.")
    class SignInCancelled : AuthError("Google ile giriş iptal edildi.")
    class SignInFailed(message: String) : AuthError(message)
}

object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCompletingRegistration = MutableStateFlow(false)
    val isCompletingRegistration: StateFlow<Boolean> = _isCompletingRegistration.asStateFlow()

    private var lastGoogleUserId: String? = null

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun ensureConfigured() = Unit

    fun setCompletingRegistration(value: Boolean) {
        _isCompletingRegistration.value = value
    }

    suspend fun signInWithGoogle(data: Intent?): GoogleSignInResult {
        if (data == null) {
            throw AuthError.SignInCancelled()
        }
        _isLoading.value = true
        try {
            val result = GoogleSignInHelper.signInWithGoogleResult(data)
            lastGoogleUserId = result.googleUserId
            return result
        } catch (error: ApiException) {
            if (error.statusCode == 12501) {
                throw AuthError.SignInCancelled()
            }
            throw AuthError.SignInFailed(googleSignInErrorMessage(error))
        } catch (error: Exception) {
            val message = error.message.orEmpty()
            if (message.contains("are blocked", ignoreCase = true)) {
                throw AuthError.SignInFailed(
                    "Google Cloud API anahtarı bu uygulamayı engelliyor. " +
                        "console.cloud.google.com → APIs & Credentials → " +
                        "\"Android key (auto created by Firebase)\" → Application restrictions → " +
                        "Android apps altına paket com.mikatechnology.BusTracker ve " +
                        "debug + release SHA-1 ekleyin (docs/ANDROID_GOOGLE_SIGNIN_FIX.md)."
                )
            }
            throw AuthError.SignInFailed(error.message ?: "Google ile giriş başarısız.")
        } finally {
            _isLoading.value = false
        }
    }

    fun resolveAuthUserId(): String? {
        val user = auth.currentUser ?: return lastGoogleUserId
        user.providerData.firstOrNull { it.providerId == "google.com" }?.uid?.let { return it }
        return user.uid
    }

    fun signOut() {
        auth.signOut()
        lastGoogleUserId = null
    }

    suspend fun deleteCurrentUser() {
        val user = auth.currentUser
            ?: throw AuthError.SignInFailed("Giriş yapılmamış.")
        user.delete().await()
        lastGoogleUserId = null
    }

    /** Google Sign-In [CommonStatusCodes] / [GoogleSignInStatusCodes] → kullanıcı mesajı. */
    private fun googleSignInErrorMessage(error: ApiException): String {
        return when (error.statusCode) {
            10 -> """
                Google yapılandırma hatası (kod 10): Bu APK'nın imza SHA-1'i Firebase'de yok.
                Play Store'dan indirdiyseniz: Play Console → App integrity → App signing key → SHA-1'i Firebase Android uygulamasına ekleyin.
                Studio/APK ile test ediyorsanız: debug veya release (Untitled.jks) SHA-1 ekli olmalı.
            """.trim().replace("\n", " ")

            7 -> "İnternet bağlantısı yok. Bağlantınızı kontrol edin."
            8 -> "Google Play Hizmetleri güncel değil veya eksik."
            12500 -> "Google ile giriş şu an kullanılamıyor. Lütfen tekrar deneyin."
            else -> {
                val detail = error.localizedMessage?.trim().orEmpty()
                if (detail.isNotBlank() && !detail.matches(Regex("^\\d+:\\s*$"))) {
                    detail
                } else {
                    "Google ile giriş başarısız (kod ${error.statusCode})."
                }
            }
        }
    }
}
