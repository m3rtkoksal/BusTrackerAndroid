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
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.services.NotificationService
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {

    init {
        configureScreen(
            title = L10n.signInAction,
            navigationBarStyle = NavigationBarStyle.NeonAuth,
            usesLargeTitle = false,
            hidesNavigationBar = true,
            embedsInNavigationStack = false
        )
    }

    fun signInWithGoogle(context: Context, data: Intent?) {
        viewModelScope.launch {
            setLoading(true, L10n.signingIn)
            try {
                AuthRepository.signInWithGoogle(data)
                val userId = AuthRepository.currentUserId
                if (userId == null) {
                    showError(L10n.signInFailed)
                    return@launch
                }

                val profile = ShuttleRepository.shared.fetchUserProfile(userId)
                if (profile != null) {
                    UserSessionRepository.save(context, profile)
                    val groupID = profile.primaryGroupID.trim()
                    if (groupID.isNotEmpty()) {
                        NotificationService.syncTokenForProfile(context, groupID, profile.memberID)
                    }
                } else {
                    showError(L10n.profileNotFound)
                    AuthRepository.signOut()
                }
            } catch (_: AuthError.SignInCancelled) {
                // User cancelled.
            } catch (error: Exception) {
                showError(error.localizedMessage ?: L10n.googleSignInFailed)
            } finally {
                setLoading(false)
            }
        }
    }
}
