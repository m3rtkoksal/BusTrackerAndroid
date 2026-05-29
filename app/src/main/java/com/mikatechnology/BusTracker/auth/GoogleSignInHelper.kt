package com.mikatechnology.BusTracker.auth

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.mikatechnology.BusTracker.R
import kotlinx.coroutines.tasks.await

data class GoogleSignInResult(
    val googleUserId: String,
    val displayName: String?,
    val email: String?
)

object GoogleSignInHelper {
    fun createSignInIntent(activity: Activity): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, options).signInIntent
    }

    suspend fun signInWithGoogleResult(data: Intent?): GoogleSignInResult {
        requireNotNull(data) { "Google sign-in intent missing." }
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)

        val idToken = account.idToken
            ?: throw IllegalStateException("Google kimlik jetonu alınamadı.")

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            FirebaseAuth.getInstance().signInWithCredential(credential).await()
        } catch (error: FirebaseAuthException) {
            val detail = error.message.orEmpty()
            if (detail.contains("blocked", ignoreCase = true)) {
                throw IllegalStateException(
                    "Firebase Auth bu APK imzasını reddetti (API key kısıtı). " +
                        "Google Cloud → Android key → Application restrictions: " +
                        "paket + debug/release SHA-1 doğru mu, veya geçici None deneyin. " +
                        "API restrictions altında Identity Toolkit API açık olmalı."
                )
            }
            throw error
        }

        val googleUserId = account.id ?: account.email
            ?: throw IllegalStateException("Google kullanıcı kimliği alınamadı.")

        return GoogleSignInResult(
            googleUserId = googleUserId,
            displayName = account.displayName,
            email = account.email
        )
    }
}
