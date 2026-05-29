package com.mikatechnology.BusTracker.ui.registration

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavSubtitleStyle
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.repository.AuthError
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleError
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import com.mikatechnology.BusTracker.services.NotificationService
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationFormViewModel(
    val role: MemberRole,
    private val shuttleRepository: ShuttleRepository = ShuttleRepository.shared
) : BaseViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _serviceField = MutableStateFlow("")
    val serviceField: StateFlow<String> = _serviceField.asStateFlow()

    private val _serviceFieldError = MutableStateFlow<String?>(null)
    val serviceFieldError: StateFlow<String?> = _serviceFieldError.asStateFlow()

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
        get() = isRegistrationFormComplete(_name.value, _serviceField.value, role)

    companion object {
        fun isRegistrationFormComplete(
            name: String,
            serviceField: String,
            role: MemberRole
        ): Boolean {
            if (name.trim().isEmpty()) return false
            val trimmedService = serviceField.trim()
            return if (role == MemberRole.Driver) {
                trimmedService.isNotEmpty()
            } else {
                // Yolcu: servis kodu girilmeden Google kayıt kapalı
                trimmedService.length >= 4
            }
        }
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
        _serviceFieldError.value = null
    }

    /** Google kayıt öncesi yerel doğrulama; hata servis alanı altında gösterilir. */
    fun validateBeforeGoogleSignIn(): Boolean {
        _serviceFieldError.value = null
        if (_name.value.trim().isEmpty()) {
            showError("Kayıt için adınızı girin.")
            return false
        }
        val trimmedService = _serviceField.value.trim()
        if (role == MemberRole.Passenger) {
            if (trimmedService.isEmpty()) {
                _serviceFieldError.value = "Servis kodu girmedin."
                return false
            }
            if (trimmedService.length < 4) {
                _serviceFieldError.value = "Servis kodu en az 4 karakter olmalı."
                return false
            }
        } else if (trimmedService.isEmpty()) {
            _serviceFieldError.value = "Servis adı girmedin."
            return false
        }
        return true
    }

    private fun serviceFieldErrorMessage(error: ShuttleError): String = when (error) {
        is ShuttleError.GroupNotFound -> error.message ?: "Bu servis kodu bulunamadı."
        is ShuttleError.InvalidInput -> error.message ?: "Geçersiz servis kodu."
        is ShuttleError.AlreadyInGroup -> error.message ?: "Zaten bir servise kayıtlısınız."
        is ShuttleError.NotAuthenticated -> error.message ?: "Giriş yapmanız gerekiyor."
    }

    private fun applyPassengerServiceFieldError(error: Exception): Boolean {
        if (role != MemberRole.Passenger) return false
        val shuttleError = error as? ShuttleError ?: return false
        _serviceFieldError.value = serviceFieldErrorMessage(shuttleError)
        return true
    }

    fun createAccountWithGoogle(context: Context, data: Intent?) {
        viewModelScope.launch {
            if (!canSubmit) {
                return@launch
            }
            AuthRepository.setCompletingRegistration(true)
            var accountCreated = false
            try {
                setLoading(true, "Hesap oluşturuluyor...")
                val googleResult = AuthRepository.signInWithGoogle(data)

                if (_name.value.trim().isEmpty() && !googleResult.displayName.isNullOrBlank()) {
                    _name.value = googleResult.displayName
                }

                if (role == MemberRole.Passenger) {
                    try {
                        shuttleRepository.validatePassengerGroupCode(_serviceField.value)
                    } catch (error: ShuttleError) {
                        _serviceFieldError.value = serviceFieldErrorMessage(error)
                        AuthRepository.signOut()
                        return@launch
                    }
                }

                val profile = if (role == MemberRole.Driver) {
                    shuttleRepository.createGroup(_serviceField.value, _name.value)
                } else {
                    shuttleRepository.joinGroup(_serviceField.value, _name.value)
                }

                UserSessionRepository.save(context, profile)
                val groupID = profile.primaryGroupID.trim()
                if (groupID.isNotEmpty()) {
                    NotificationService.syncTokenForProfile(context, groupID, profile.memberID)
                }
                accountCreated = true
                showSuccess(
                    if (role == MemberRole.Driver) {
                        "Servis hesabınız oluşturuldu."
                    } else {
                        "Servise katıldınız."
                    }
                )
            } catch (_: AuthError.SignInCancelled) {
                // Kullanıcı iptal etti.
            } catch (error: Exception) {
                if (!applyPassengerServiceFieldError(error)) {
                    showError(error.localizedMessage ?: "Hesap oluşturulamadı.")
                }
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
