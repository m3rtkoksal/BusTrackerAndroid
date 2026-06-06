package com.mikatechnology.BusTracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import com.mikatechnology.BusTracker.localization.LanguageManager
import com.mikatechnology.BusTracker.ui.driver.DriverHomeView
import com.mikatechnology.BusTracker.ui.passenger.PassengerHomeView
import com.mikatechnology.BusTracker.ui.registration.RegistrationFlowScreen
import com.mikatechnology.BusTracker.ui.registration.LoginScreen

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val profile by UserSessionRepository.profile.collectAsStateWithLifecycle()
    val isSessionLoaded by UserSessionRepository.isSessionLoaded.collectAsStateWithLifecycle()
    val appLanguage by LanguageManager.language.collectAsStateWithLifecycle()

    var showLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UserSessionRepository.load(context)
        val localProfile = UserSessionRepository.profile.value ?: return@LaunchedEffect
        if (!AuthRepository.isSignedIn) {
            UserSessionRepository.clear(context)
            return@LaunchedEffect
        }
        try {
            val remoteProfile = ShuttleRepository.shared.fetchUserProfile(localProfile.userID)
            if (remoteProfile == null) {
                UserSessionRepository.signOut(context)
            } else {
                UserSessionRepository.save(context, remoteProfile)
            }
        } catch (_: Exception) {
            // Ağ hatası: yerel oturumu koru.
        }
    }

    AppPermissionsHandler(
        enabled = isSessionLoaded && profile != null,
        profile = profile,
        notificationsOnly = profile?.role == MemberRole.Driver
    )

    if (!isSessionLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    key(appLanguage) {
        when (val currentProfile = profile) {
            null -> {
                if (showLogin) {
                    LoginScreen(
                        onBackToRegister = { showLogin = false }
                    )
                } else {
                    RegistrationFlowScreen(
                        onLoginTapped = { showLogin = true }
                    )
                }
            }

            else -> when (currentProfile.role) {
                MemberRole.Driver -> DriverHomeView(profile = currentProfile)
                MemberRole.Passenger -> PassengerHomeView(profile = currentProfile)
            }
        }
    }
}
