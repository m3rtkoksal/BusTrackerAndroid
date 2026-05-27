package com.mikatechnology.BusTracker.ui.registration

import android.app.Activity
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _showOTP = MutableStateFlow(false)
    val showOTP: StateFlow<Boolean> = _showOTP.asStateFlow()

    init {
        configureScreen(
            title = "Giriş Yap",
            navigationBarStyle = NavigationBarStyle.NeonAuth,
            usesLargeTitle = false,
            hidesNavigationBar = true,
            embedsInNavigationStack = false
        )
    }

    val canSubmit: Boolean
        get() = _phone.value.filter { it.isDigit() }.length >= 10

    val formattedPhone: String
        get() = AuthRepository.displayFormat(AuthRepository.formatPhone(_phone.value))

    fun onPhoneChange(value: String) {
        _phone.value = value
    }

    fun onOtpChange(value: String) {
        _otpCode.value = value.filter { it.isDigit() }.take(6)
    }

    fun sendLoginOTP(activity: Activity) {
        viewModelScope.launch {
            try {
                AuthRepository.sendOTP(activity, _phone.value)

                // Always show the OTP sheet in the login flow.
                // This ensures test phone numbers (which often do instant verification)
                // still show the sheet so the user can enter the fixed test code.
                _showOTP.value = true
                _otpCode.value = ""
            } catch (e: Exception) {
                showError(e.message ?: "Doğrulama kodu gönderilemedi")
            }
        }
    }

    fun verifyLogin(context: Context) {
        viewModelScope.launch {
            try {
                // For test phone numbers, sendOTP often does instant verification.
                // In that case we are already signed in, so skip verifyOTP
                // (calling it would fail with "MissingVerificationId").
                if (!AuthRepository.isSignedIn) {
                    AuthRepository.verifyOTP(_otpCode.value)
                }

                // Fetch the actual profile from Firestore (this is what was missing for login)
                val userId = AuthRepository.currentUserId
                if (userId != null) {
                    val profile = ShuttleRepository().fetchUserProfile(userId)
                    if (profile != null) {
                        UserSessionRepository.save(context, profile)
                        // Close the sheet — AppRoot will react to the profile change and navigate
                        _showOTP.value = false
                        _otpCode.value = ""
                    } else {
                        showError("Bu telefon numarasıyla kayıtlı bir profil bulunamadı. Önce kayıt olmalısınız.")
                    }
                } else {
                    showError("Giriş yapılamadı (kullanıcı bilgisi alınamadı).")
                }

            } catch (e: Exception) {
                showError(e.message ?: "Giriş yapılamadı. Kodu kontrol edin.")
            }
        }
    }

    fun dismissOTP() {
        _showOTP.value = false
        _otpCode.value = ""
    }
}
