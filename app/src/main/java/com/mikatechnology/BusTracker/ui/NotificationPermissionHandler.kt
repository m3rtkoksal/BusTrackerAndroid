package com.mikatechnology.BusTracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.services.NotificationService
import kotlinx.coroutines.launch

/**
 * Uygulama açılışında ve ön plana gelince bildirim iznini kontrol eder.
 * Kapalıysa sistem diyaloğu veya Ayarlar yönlendirmesi gösterir.
 */
@Composable
fun NotificationPermissionHandler(
    enabled: Boolean,
    profile: UserProfile? = null
) {
    if (!enabled) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var pendingResumeCheck by remember { mutableStateOf(false) }

    fun syncTokenIfPossible() {
        val current = profile ?: return
        val groupID = current.primaryGroupID.trim()
        if (groupID.isEmpty()) return
        scope.launch {
            NotificationService.syncTokenForProfile(context, groupID, current.memberID)
        }
    }

    fun onPermissionResolved(granted: Boolean) {
        if (granted) {
            syncTokenIfPossible()
        } else {
            showSettingsDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onPermissionResolved(granted) }

    fun promptNotifications() {
        NotificationService.createNotificationChannels(context)

        if (NotificationService.hasNotificationPermission(context)) {
            syncTokenIfPossible()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val enabledInSystem = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (enabledInSystem) {
            syncTokenIfPossible()
        } else {
            showSettingsDialog = true
        }
    }

    DisposableEffect(lifecycleOwner, enabled, profile?.memberID) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                pendingResumeCheck = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.compose.runtime.LaunchedEffect(enabled, profile?.memberID) {
        if (!enabled || profile == null) return@LaunchedEffect
        if (NotificationService.hasNotificationPermission(context)) {
            syncTokenIfPossible()
        }
    }

    androidx.compose.runtime.LaunchedEffect(enabled, pendingResumeCheck) {
        if (!enabled || !pendingResumeCheck || profile == null) return@LaunchedEffect
        pendingResumeCheck = false
        if (NotificationService.hasNotificationPermission(context)) {
            syncTokenIfPossible()
        } else {
            promptNotifications()
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(L10n.notificationsDisabledTitle) },
            text = { Text(L10n.notificationsDisabledMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    NotificationService.openAppSettings(context)
                }) {
                    Text(L10n.openSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(L10n.later)
                }
            }
        )
    }
}
