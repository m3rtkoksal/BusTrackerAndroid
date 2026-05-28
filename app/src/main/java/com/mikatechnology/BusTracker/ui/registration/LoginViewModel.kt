package com.mikatechnology.BusTracker.ui.registration

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.repository.AuthError
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {

    init {
        configureScreen(
            title = "Giriş Yap",
            navigationBarStyle = NavigationBarStyle.NeonAuth,
            usesLargeTitle = false,
            hidesNavigationBar = true,
            embedsInNavigationStack = false
        )
    }

    fun signInWithGoogle(context: Context, data: Intent?) {
        viewModelScope.launch {
            setLoading(true, "Giriş yapılıyor...")
            try {
                AuthRepository.signInWithGoogle(data)
                val userId = AuthRepository.currentUserId
                if (userId == null) {
                    showError("Giriş yapılamadı.")
                    return@launch
                }

                val profile = ShuttleRepository.shared.fetchUserProfile(userId)
                if (profile != null) {
                    UserSessionRepository.save(context, profile)
                } else {
                    showError("Bu Google hesabıyla kayıtlı profil bulunamadı. Önce hesap oluşturun.")
                    AuthRepository.signOut()
                }
            } catch (_: AuthError.SignInCancelled) {
                // Kullanıcı iptal etti.
            } catch (error: Exception) {
                showError(error.localizedMessage ?: "Google ile giriş başarısız.")
            } finally {
                setLoading(false)
            }
        }
    }
}
