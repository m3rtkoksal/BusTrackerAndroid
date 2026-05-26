package com.mikatechnology.BusTracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import com.mikatechnology.BusTracker.ui.driver.DriverHomeView
import com.mikatechnology.BusTracker.ui.registration.RegistrationFlowScreen

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val profile by UserSessionRepository.profile.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        UserSessionRepository.load(context)
    }

    when (val currentProfile = profile) {
        null -> {
            RegistrationFlowScreen(
                onLoginTapped = {
                    // Login ekranı sonraki adım
                }
            )
        }

        else -> when (currentProfile.role) {
            MemberRole.Driver -> DriverHomeView(profile = currentProfile)
            MemberRole.Passenger -> LoggedInPlaceholderScreen(profile = currentProfile)
        }
    }
}
