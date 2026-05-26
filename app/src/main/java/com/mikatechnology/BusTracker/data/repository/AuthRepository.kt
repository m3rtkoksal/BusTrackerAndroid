package com.mikatechnology.BusTracker.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mikatechnology.BusTracker.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class AuthError(message: String) : Exception(message) {
    class InvalidPhone : AuthError("Geçerli bir telefon numarası girin.")
    class MissingVerificationId : AuthError("Önce doğrulama kodu isteyin.")
    class FirebaseNotReady : AuthError("Firebase henüz hazır değil. Lütfen tekrar deneyin.")
}

object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private var verificationId: String? = null
    private var verifiedPhoneNumber: String? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCompletingRegistration = MutableStateFlow(false)
    val isCompletingRegistration: StateFlow<Boolean> = _isCompletingRegistration.asStateFlow()

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val displayPhoneNumber: String?
        get() = auth.currentUser?.phoneNumber ?: verifiedPhoneNumber

    fun ensureConfigured() {
        if (BuildConfig.DEBUG) {
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        }
    }

    fun setCompletingRegistration(value: Boolean) {
        _isCompletingRegistration.value = value
    }

    suspend fun sendOTP(activity: Activity, rawPhone: String) {
        val formatted = formatPhone(rawPhone)
        if (formatted.filter { it.isDigit() }.length < 12) {
            throw AuthError.InvalidPhone()
        }

        _isLoading.value = true
        try {
            verifiedPhoneNumber = formatted
            val id = suspendCancellableCoroutine<String?> { continuation ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        auth.signInWithCredential(credential)
                            .addOnSuccessListener {
                                verifiedPhoneNumber = auth.currentUser?.phoneNumber ?: verifiedPhoneNumber
                                verificationId = null
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                            .addOnFailureListener { error ->
                                if (continuation.isActive) {
                                    continuation.resumeWithException(error)
                                }
                            }
                    }

                    override fun onVerificationFailed(error: FirebaseException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }

                    override fun onCodeSent(
                        id: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(id)
                        }
                    }
                }

                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(formatted)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            }
            verificationId = id
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun verifyOTP(code: String) {
        val id = verificationId ?: throw AuthError.MissingVerificationId()

        _isLoading.value = true
        try {
            val credential = PhoneAuthProvider.getCredential(id, code.trim())
            suspendCancellableCoroutine { continuation ->
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        verifiedPhoneNumber = auth.currentUser?.phoneNumber ?: verifiedPhoneNumber
                        verificationId = null
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }
        } finally {
            _isLoading.value = false
        }
    }

    fun signOut() {
        auth.signOut()
        verificationId = null
        verifiedPhoneNumber = null
    }

    fun formatPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("90") && digits.length == 12 -> "+$digits"
            digits.startsWith("0") && digits.length == 11 -> "+9$digits"
            digits.length == 10 -> "+90$digits"
            raw.startsWith("+") -> "+$digits"
            else -> "+$digits"
        }
    }

    fun displayFormat(e164: String): String {
        val digits = e164.filter { it.isDigit() }
        if (digits.length < 12 || !digits.startsWith("90")) return e164
        val local = digits.drop(2)
        if (local.length != 10) return e164
        val area = local.take(3)
        val mid = local.drop(3).take(3)
        val end = local.takeLast(4)
        return "+90 $area $mid ${end.take(2)} ${end.takeLast(2)}"
    }
}
