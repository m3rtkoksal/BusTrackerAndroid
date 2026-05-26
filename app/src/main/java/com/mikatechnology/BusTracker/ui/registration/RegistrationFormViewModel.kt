package com.mikatechnology.BusTracker.ui.registration

import android.app.Activity
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavSubtitleStyle
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationFormViewModel(
    val role: MemberRole,
    private val shuttleRepository: ShuttleRepository = ShuttleRepository.shared
) : BaseViewModel() {

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _serviceField = MutableStateFlow("")
    val serviceField: StateFlow<String> = _serviceField.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _showOTPVerification = MutableStateFlow(false)
    val showOTPVerification: StateFlow<Boolean> = _showOTPVerification.asStateFlow()

    init {
        configureScreen(
            title = "Hesap Oluştur",
            navSubtitleStyle = NavSubtitleStyle.Hidden,
            navigationBarStyle = NavigationBarStyle.NeonAuth,
            usesLargeTitle = false,
            hidesNavigationBar = true,
            embedsInNavigationStack = false,
            usesCustomNavHeader = false
        )
    }

    val heroTitle: String
        get() = if (role == MemberRole.Driver) "Sürücü Kaydı" else "Yolcu Kaydı"

    val heroSubtitle: String
        get() = if (role == MemberRole.Driver) {
            "Servisinizi oluşturun, yolcularınız sizi takip etsin."
        } else {
            "Servis kodunuzla katılın, haritadan takip edin."
        }

    val accent: Color
        get() = if (role == MemberRole.Driver) NeonTheme.Primary else NeonTheme.Secondary

    val serviceFieldTitle: String
        get() = if (role == MemberRole.Driver) "Servis adı" else "Servis kodu"

    val serviceFieldPrompt: String
        get() = if (role == MemberRole.Driver) {
            "Örn. Kadıköy Servisi"
        } else {
            "Sürücünün verdiği 6 haneli kod"
        }

    val namePrompt: String
        get() = if (role == MemberRole.Driver) "Örn. Ahmet" else "Örn. Ayşe"

    val footerCaption: String
        get() = if (role == MemberRole.Driver) {
            "Konum paylaşımı yalnızca sürücü hesabında açıktır."
        } else {
            "Yolcu hesabında konum paylaşımı yoktur."
        }

    val canSubmit: Boolean
        get() {
            if (_phone.value.filter { it.isDigit() }.length < 10) return false
            if (_name.value.trim().isEmpty()) return false
            val trimmed = _serviceField.value.trim()
            return if (role == MemberRole.Driver) {
                trimmed.isNotEmpty()
            } else {
                _serviceField.value.length >= 4
            }
        }

    val formattedPhone: String
        get() = AuthRepository.displayFormat(AuthRepository.formatPhone(_phone.value))

    fun onPhoneChange(value: String) {
        _phone.value = value
    }

    fun onNameChange(value: String) {
        _name.value = value
    }

    fun onServiceFieldChange(value: String) {
        _serviceField.value = if (role == MemberRole.Passenger) {
            value.uppercase()
        } else {
            value
        }
    }

    fun onOtpChange(value: String) {
        _otpCode.value = value
    }

    fun dismissOTP() {
        _showOTPVerification.value = false
        _otpCode.value = ""
    }

    fun beginAccountCreation(activity: Activity) {
        viewModelScope.launch {
            try {
                AuthRepository.sendOTP(activity, _phone.value)
                if (AuthRepository.isSignedIn) {
                    verifyAndCreateAccount(activity.applicationContext)
                } else {
                    _otpCode.value = ""
                    _showOTPVerification.value = true
                }
            } catch (error: Exception) {
                showError(error.localizedMessage ?: "OTP gönderilemedi.")
            }
        }
    }

    fun verifyAndCreateAccount(context: Context) {
        viewModelScope.launch {
            AuthRepository.setCompletingRegistration(true)
            var accountCreated = false
            try {
                if (!AuthRepository.isSignedIn) {
                    AuthRepository.verifyOTP(_otpCode.value)
                }
                _showOTPVerification.value = false
                _otpCode.value = ""
                setLoading(true, "Hesap oluşturuluyor...")

                val profile = if (role == MemberRole.Driver) {
                    shuttleRepository.createGroup(_serviceField.value, _name.value)
                } else {
                    shuttleRepository.joinGroup(_serviceField.value, _name.value)
                }

                UserSessionRepository.save(context, profile)
                accountCreated = true
                showSuccess(
                    if (role == MemberRole.Driver) {
                        "Servis hesabınız oluşturuldu."
                    } else {
                        "Servise katıldınız."
                    }
                )
            } catch (error: Exception) {
                showError(error.localizedMessage ?: "Hesap oluşturulamadı.")
                if (UserSessionRepository.profile.value == null) {
                    AuthRepository.signOut()
                }
            } finally {
                AuthRepository.setCompletingRegistration(false)
                if (!accountCreated) {
                    setLoading(false)
                }
            }
        }
    }
}
