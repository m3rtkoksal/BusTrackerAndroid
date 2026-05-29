package com.mikatechnology.BusTracker.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.services.NotificationService
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
    val scope = rememberCoroutineScope()

    val foregroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val role = when (profile?.role) {
            MemberRole.Driver -> LocationPermissionRole.Driver
            else -> LocationPermissionRole.Passenger
        }
        LocationTracker.refreshAuthorizationStatus(context, role)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val current = profile
            if (current != null) {
                scope.launch { syncPushToken(context, current) }
            }
        }
    }

    LaunchedEffect(Unit) {
        UserSessionRepository.load(context)
    }

    // Giriş veya kayıt ekranına gelir gelmez: "Uygulama kullanılırken" konum izni
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

    LaunchedEffect(profile?.memberID, profile?.primaryGroupID) {
        val current = profile ?: return@LaunchedEffect
        NotificationService.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationService.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return@LaunchedEffect
        }
        syncPushToken(context, current)
    }

    // Wait until we have checked local session before deciding what to show
    if (!isSessionLoaded) {
        // Simple loading state (you can replace with a proper splash later)
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

private suspend fun syncPushToken(context: android.content.Context, profile: UserProfile) {
    val groupID = profile.primaryGroupID.trim()
    if (groupID.isEmpty()) return
    NotificationService.syncTokenForProfile(context, groupID, profile.memberID)
}
