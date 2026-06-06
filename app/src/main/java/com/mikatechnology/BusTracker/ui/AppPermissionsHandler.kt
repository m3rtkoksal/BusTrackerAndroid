package com.mikatechnology.BusTracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.services.MotionActivityService
import com.mikatechnology.BusTracker.services.NotificationService
import com.mikatechnology.BusTracker.services.PermissionPromptSession
import com.mikatechnology.BusTracker.ui.driver.DriverNotificationGuideSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Sistem popup'ı kapandıktan sonra sıradaki izne geçmek için kısa bekleme. */
private const val PERMISSION_DIALOG_SETTLE_MS = 500L

/** Red sonrası bottom sheet — sistem diyaloğu kapanmadan önce flash olmasın (iOS ~450ms). */
private const val NOTIFICATION_GUIDE_DEFER_MS = 450L

/**
 * Açılış izinleri: sürücü → yalnızca bildirim (sistem → red ise bottom sheet, her cold launch).
 * Yolcu launch → (ayrı akış). Sürücü konum/hareket → Servisi başlat bottom sheet zinciri.
 */
@Composable
fun AppPermissionsHandler(
    enabled: Boolean,
    profile: UserProfile? = null,
    notificationsOnly: Boolean = false
) {
    if (!enabled) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var flowBusy by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<PermissionAction?>(null) }
    var resumeQueued by remember { mutableStateOf(false) }
    var showNotificationGuide by remember { mutableStateOf(false) }
    var notificationGuideWaitingSettings by remember { mutableStateOf(false) }

    val locationRole = when (profile?.role) {
        MemberRole.Driver -> LocationPermissionRole.Driver
        else -> LocationPermissionRole.Passenger
    }

    fun syncNotificationTokenIfPossible() {
        val current = profile ?: return
        val groupID = current.primaryGroupID.trim()
        if (groupID.isEmpty()) return
        scope.launch {
            NotificationService.syncTokenForProfile(context, groupID, current.memberID)
        }
    }

    fun dismissNotificationGuide() {
        notificationGuideWaitingSettings = false
        showNotificationGuide = false
    }

    fun refreshNotificationGuideAfterSettingsReturn() {
        if (!showNotificationGuide) return
        if (!NotificationService.hasNotificationPermission(context)) return
        dismissNotificationGuide()
        syncNotificationTokenIfPossible()
    }

    fun presentNotificationSettingsGuide() {
        notificationGuideWaitingSettings = false
        showNotificationGuide = true
        PermissionPromptSession.markNotificationPromptHandled()
        flowBusy = false
    }

    fun scheduleNextStep() {
        scope.launch {
            delay(PERMISSION_DIALOG_SETTLE_MS)
            flowBusy = false
            resumeQueued = true
        }
    }

    suspend fun awaitDriverAlwaysLocation(maxWaitMs: Long = 15_000L): Boolean {
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (System.currentTimeMillis() < deadline) {
            if (LocationTracker.hasDriverAlwaysLocation(context)) return true
            delay(150)
        }
        return LocationTracker.hasDriverAlwaysLocation(context)
    }

    fun onStepFinished(granted: Boolean, step: PermissionStep) {
        pendingAction = null
        flowBusy = true
        scope.launch {
            var resolved = granted
            if (step == PermissionStep.LocationBackground && !resolved) {
                resolved = awaitDriverAlwaysLocation()
            }
            if (step == PermissionStep.Notifications) {
                PermissionPromptSession.markNotificationPromptHandled()
                if (!resolved && NotificationService.shouldShowNotificationSettingsGuide(context)) {
                    delay(NOTIFICATION_GUIDE_DEFER_MS)
                    presentNotificationSettingsGuide()
                    return@launch
                }
            }
            if (resolved) {
                if (step == PermissionStep.Notifications) {
                    syncNotificationTokenIfPossible()
                }
            }
            scheduleNextStep()
        }
    }

    fun beginPermissionStep() {
        if (flowBusy || pendingAction != null) return

        NotificationService.createNotificationChannels(context)
        LocationTracker.initialize(context)
        MotionActivityService.initialize(context)
        LocationTracker.refreshAuthorizationStatus(context, locationRole)

        when (nextMissingPermissionStep(context, profile, notificationsOnly)) {
            PermissionStep.LocationForeground -> {
                PermissionPromptSession.markLocationPromptHandled()
                flowBusy = true
                pendingAction = PermissionAction.RequestLocationForeground
            }
            PermissionStep.LocationBackground -> {
                PermissionPromptSession.markLocationPromptHandled()
                flowBusy = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pendingAction = PermissionAction.RequestLocationBackground
                } else {
                    scheduleNextStep()
                }
            }
            PermissionStep.Motion -> {
                PermissionPromptSession.markMotionPromptHandled()
                flowBusy = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pendingAction = PermissionAction.RequestMotion
                } else {
                    scheduleNextStep()
                }
            }
            PermissionStep.Notifications -> {
                if (NotificationService.hasNotificationPermission(context)) {
                    syncNotificationTokenIfPossible()
                    return
                }
                if (!PermissionPromptSession.mayPromptNotifications) {
                    flowBusy = false
                    return
                }
                flowBusy = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (NotificationService.shouldShowNotificationSettingsGuide(context)) {
                        presentNotificationSettingsGuide()
                    } else {
                        NotificationService.markNotificationPermissionRequested(context)
                        pendingAction = PermissionAction.RequestNotification
                    }
                } else if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    PermissionPromptSession.markNotificationPromptHandled()
                    syncNotificationTokenIfPossible()
                    flowBusy = false
                } else {
                    PermissionPromptSession.markNotificationPromptHandled()
                    presentNotificationSettingsGuide()
                }
            }
            PermissionStep.Done -> {
                syncNotificationTokenIfPossible()
                flowBusy = false
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onStepFinished(granted, PermissionStep.Notifications) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        LocationTracker.refreshAuthorizationStatus(context, locationRole)
        val granted = results.values.any { it }
        if (!granted) {
            onStepFinished(false, PermissionStep.LocationForeground)
            return@rememberLauncherForActivityResult
        }
        if (profile?.role == MemberRole.Driver &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !LocationTracker.hasDriverAlwaysLocation(context)
        ) {
            flowBusy = true
            pendingAction = PermissionAction.RequestLocationBackground
            return@rememberLauncherForActivityResult
        }
        onStepFinished(true, PermissionStep.LocationForeground)
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
            var ok = granted || LocationTracker.hasDriverAlwaysLocation(context)
            if (!ok) {
                ok = awaitDriverAlwaysLocation()
            }
            onStepFinished(ok, PermissionStep.LocationBackground)
        }
    }

    val motionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onStepFinished(granted, PermissionStep.Motion) }

    androidx.compose.runtime.LaunchedEffect(pendingAction) {
        when (pendingAction) {
            PermissionAction.RequestNotification ->
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            PermissionAction.RequestLocationForeground ->
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            PermissionAction.RequestLocationBackground ->
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            PermissionAction.RequestMotion ->
                motionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            null -> Unit
        }
    }

    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshNotificationGuideAfterSettingsReturn()
                resumeQueued = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.compose.runtime.LaunchedEffect(enabled, profile?.memberID, notificationsOnly) {
        if (enabled) resumeQueued = true
    }

    androidx.compose.runtime.LaunchedEffect(enabled, resumeQueued, profile?.memberID, notificationsOnly) {
        if (!enabled || !resumeQueued) return@LaunchedEffect
        resumeQueued = false
        LocationTracker.refreshAuthorizationStatus(context, locationRole)
        if (flowBusy || pendingAction != null) return@LaunchedEffect
        beginPermissionStep()
    }

    if (showNotificationGuide) {
        Dialog(
            onDismissRequest = { dismissNotificationGuide() },
            properties = DialogProperties(
                decorFitsSystemWindows = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { dismissNotificationGuide() },
                contentAlignment = Alignment.BottomCenter
            ) {
                DriverNotificationGuideSheet(
                    waitingForSettingsReturn = notificationGuideWaitingSettings,
                    onOpenSettings = {
                        notificationGuideWaitingSettings = true
                        NotificationService.openAppSettings(context)
                    },
                    onDismiss = { dismissNotificationGuide() }
                )
            }
        }
    }
}

private enum class PermissionStep {
    LocationForeground,
    LocationBackground,
    Motion,
    Notifications,
    Done
}

private enum class PermissionAction {
    RequestLocationForeground,
    RequestLocationBackground,
    RequestMotion,
    RequestNotification
}

/** Sürücü: konum → hareket → bildirim. Yolcu: açılışta izin yok (harita + aksiyon zinciri). */
private fun nextMissingPermissionStep(
    context: android.content.Context,
    profile: UserProfile?,
    notificationsOnly: Boolean
): PermissionStep {
    if (!notificationsOnly && profile?.role != MemberRole.Passenger) {
        if (PermissionPromptSession.mayPromptLocation) {
            if (!LocationTracker.hasFineLocation(context)) {
                return PermissionStep.LocationForeground
            }
            if (profile?.role == MemberRole.Driver &&
                !LocationTracker.hasDriverAlwaysLocation(context)
            ) {
                return PermissionStep.LocationBackground
            }
        }
        if (PermissionPromptSession.mayPromptMotion &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !MotionActivityService.hasActivityRecognitionPermission(context)
        ) {
            return PermissionStep.Motion
        }
    }
    if (!NotificationService.hasNotificationPermission(context) &&
        PermissionPromptSession.mayPromptNotifications &&
        profile != null &&
        profile.role != MemberRole.Passenger
    ) {
        return PermissionStep.Notifications
    }
    return PermissionStep.Done
}
