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

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _serviceField = MutableStateFlow("")
    val serviceField: StateFlow<String> = _serviceField.asStateFlow()

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
            if (_name.value.trim().isEmpty()) return false
            val trimmed = _serviceField.value.trim()
            return if (role == MemberRole.Driver) {
                trimmed.isNotEmpty()
            } else {
                _serviceField.value.length >= 4
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
    }

    fun createAccountWithGoogle(context: Context, data: Intent?) {
        viewModelScope.launch {
            AuthRepository.setCompletingRegistration(true)
            var accountCreated = false
            try {
                setLoading(true, "Hesap oluşturuluyor...")
                val googleResult = AuthRepository.signInWithGoogle(data)

                if (_name.value.trim().isEmpty() && !googleResult.displayName.isNullOrBlank()) {
                    _name.value = googleResult.displayName
                }

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
            } catch (_: AuthError.SignInCancelled) {
                // Kullanıcı iptal etti.
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
