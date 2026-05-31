package com.mikatechnology.BusTracker.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.ui.driver.DriverHomeView
import com.mikatechnology.BusTracker.ui.passenger.PassengerHomeView
import com.mikatechnology.BusTracker.ui.registration.RegistrationFlowScreen
import com.mikatechnology.BusTracker.ui.registration.LoginScreen

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val profile by UserSessionRepository.profile.collectAsStateWithLifecycle()
    val isSessionLoaded by UserSessionRepository.isSessionLoaded.collectAsStateWithLifecycle()

    var showLogin by remember { mutableStateOf(false) }

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val role = when (profile?.role) {
            MemberRole.Driver -> LocationPermissionRole.Driver
            else -> LocationPermissionRole.Passenger
        }
        LocationTracker.refreshAuthorizationStatus(context, role)
    }

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

    LaunchedEffect(isSessionLoaded, profile?.userID) {
        if (!isSessionLoaded) return@LaunchedEffect
        LocationTracker.initialize(context)
        if (!LocationTracker.hasFineLocation(context)) {
            foregroundLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            val role = when (profile?.role) {
                MemberRole.Driver -> LocationPermissionRole.Driver
                else -> LocationPermissionRole.Passenger
            }
            LocationTracker.refreshAuthorizationStatus(context, role)
        }
    }

    NotificationPermissionHandler(
        enabled = isSessionLoaded && profile != null,
        profile = profile
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
