package com.mikatechnology.BusTracker.ui.passenger

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.services.DriverStartPermissionPrefs
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.services.MotionActivityService
import com.mikatechnology.BusTracker.services.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PassengerActionPermissionSheet {
    Notification,
    LocationForeground,
    Motion
}

sealed class PassengerPendingGatedAction {
    data object SavePickup : PassengerPendingGatedAction()
    data class UpdateAttendance(val status: AttendanceStatus) : PassengerPendingGatedAction()
}

/**
 * iOS PassengerHomeViewModel izin akışının Android karşılığı.
 * Bildirim → Konum → Hareket → Aksiyon sırası (iOS parity).
 */
class PassengerActionPermissionManager {

    private val _activeSheet = MutableStateFlow<PassengerActionPermissionSheet?>(null)
    val activeSheet: StateFlow<PassengerActionPermissionSheet?> = _activeSheet.asStateFlow()

    private var pendingGatedAction: PassengerPendingGatedAction? = null
    private var isRequestingNotification = false
    private var isRequestingLocation = false
    private var isRequestingMotion = false
    private var didRequestMotionForAction = false

    val hasPendingAction: Boolean get() = pendingGatedAction != null

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun beginGatedAction(
        context: Context,
        action: PassengerPendingGatedAction,
        requestNotification: () -> Unit,
        requestLocation: () -> Unit,
        requestMotion: () -> Unit,
        onComplete: (PassengerPendingGatedAction) -> Unit
    ) {
        pendingGatedAction = action
        didRequestMotionForAction = false
        runPermissionFlow(context, requestNotification, requestLocation, requestMotion, onComplete)
    }

    fun dismissFlow() {
        pendingGatedAction = null
        _activeSheet.value = null
        isRequestingNotification = false
        isRequestingLocation = false
        isRequestingMotion = false
        didRequestMotionForAction = false
    }

    /** Bildirim sistem diyaloğu sonucu */
    fun onNotificationPermissionResult(
        context: Context,
        granted: Boolean,
        requestNotification: () -> Unit,
        requestLocation: () -> Unit,
        requestMotion: () -> Unit,
        onComplete: (PassengerPendingGatedAction) -> Unit
    ) {
        isRequestingNotification = false
        NotificationService.markNotificationPermissionRequested(context)

        if (granted) {
            // İzin verildi, akışa devam
            runPermissionFlow(context, requestNotification, requestLocation, requestMotion, onComplete)
        } else {
            // Reddedildi, bottom sheet göster
            if (pendingGatedAction != null) {
                _activeSheet.value = PassengerActionPermissionSheet.Notification
            }
        }
    }

    /** Konum sistem diyaloğu sonucu */
    fun onLocationPermissionResult(
        context: Context,
        requestNotification: () -> Unit,
        requestLocation: () -> Unit,
        requestMotion: () -> Unit,
        onComplete: (PassengerPendingGatedAction) -> Unit
    ) {
        isRequestingLocation = false
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)

        if (LocationTracker.hasFineLocation(context)) {
            // İzin verildi, akışa devam
            LocationTracker.requestSingleLocation(context)
            runPermissionFlow(context, requestNotification, requestLocation, requestMotion, onComplete)
        } else {
            // Reddedildi, bottom sheet göster
            if (pendingGatedAction != null) {
                _activeSheet.value = PassengerActionPermissionSheet.LocationForeground
            }
        }
    }

    /** Hareket sistem diyaloğu sonucu */
    fun onMotionPermissionResult(
        context: Context,
        requestNotification: () -> Unit,
        requestLocation: () -> Unit,
        requestMotion: () -> Unit,
        onComplete: (PassengerPendingGatedAction) -> Unit
    ) {
        isRequestingMotion = false
        MotionActivityService.refreshAuthorization(context)

        if (MotionActivityService.hasActivityRecognitionPermission(context)) {
            // İzin verildi, akışa devam
            runPermissionFlow(context, requestNotification, requestLocation, requestMotion, onComplete)
        } else {
            // Reddedildi, bottom sheet göster
            if (pendingGatedAction != null) {
                _activeSheet.value = PassengerActionPermissionSheet.Motion
            }
        }
    }

    /** Ayarlardan dönüşte veya lifecycle resume'da çağrılır */
    fun onPermissionsUpdated(
        context: Context,
        requestNotification: () -> Unit,
        requestLocation: () -> Unit,
        requestMotion: () -> Unit,
        onComplete: (PassengerPendingGatedAction) -> Unit
    ) {
        if (pendingGatedAction == null) return
        if (isRequestingNotification || isRequestingLocation || isRequestingMotion) return
        runPermissionFlow(context, requestNotification, requestLocation, requestMotion, onComplete)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permission Flow (iOS parity)
    // ─────────────────────────────────────────────────────────────────────────

    private fun runPermissionFlow(
        context: Context,
        requestNotification: () -> Unit,
        requestLocation: () -> Unit,
        requestMotion: () -> Unit,
        onComplete: (PassengerPendingGatedAction) -> Unit
    ) {
        val action = pendingGatedAction ?: return

        // 1. Bildirim gate
        if (requiresNotificationGate(action)) {
            if (!passNotificationGate(context, requestNotification)) return
        }

        // 2. Konum gate
        if (!passLocationGate(context, requestLocation)) return

        // 3. Hareket gate
        if (!passMotionGate(context, requestMotion)) return

        // Tüm izinler tamam, aksiyonu çalıştır
        _activeSheet.value = null
        pendingGatedAction = null
        didRequestMotionForAction = false
        onComplete(action)
    }

    private fun requiresNotificationGate(action: PassengerPendingGatedAction): Boolean = when (action) {
        PassengerPendingGatedAction.SavePickup -> true
        is PassengerPendingGatedAction.UpdateAttendance -> action.status == AttendanceStatus.Coming
    }

    /**
     * Bildirim izni kontrolü (iOS parity):
     * - İzin varsa: true döner, akış devam
     * - notDetermined: sistem diyaloğu göster, false döner (callback'te devam)
     * - denied: sheet göster, false döner
     */
    private fun passNotificationGate(context: Context, requestNotification: () -> Unit): Boolean {
        if (pendingGatedAction == null) return false

        // İzin zaten varsa geç
        if (NotificationService.hasNotificationPermission(context)) {
            if (_activeSheet.value == PassengerActionPermissionSheet.Notification) {
                _activeSheet.value = null
            }
            return true
        }

        // Android 13+ için POST_NOTIFICATIONS izni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Daha önce sistem diyaloğu gösterildi mi?
            if (!NotificationService.shouldShowNotificationSettingsGuide(context)) {
                // Henüz gösterilmedi, sistem diyaloğu göster
                if (isRequestingNotification) return false
                isRequestingNotification = true
                _activeSheet.value = null
                requestNotification()
                return false
            }
            // Daha önce gösterildi ve reddedildi, sheet göster
            _activeSheet.value = PassengerActionPermissionSheet.Notification
            return false
        }

        // Android 12 ve altı - uygulama ayarlarından kontrol
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            _activeSheet.value = PassengerActionPermissionSheet.Notification
            return false
        }

        return true
    }

    /**
     * Konum izni kontrolü (iOS parity):
     * - İzin varsa: true döner, akış devam
     * - notDetermined: sistem diyaloğu göster, false döner (callback'te devam)
     * - denied: sheet göster, false döner
     */
    private fun passLocationGate(context: Context, requestLocation: () -> Unit): Boolean {
        if (pendingGatedAction == null) return false

        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)

        // İzin zaten varsa geç
        if (LocationTracker.hasFineLocation(context)) {
            if (_activeSheet.value == PassengerActionPermissionSheet.LocationForeground) {
                _activeSheet.value = null
            }
            return true
        }

        // Konum durumunu kontrol et
        val status = LocationTracker.authorizationStatus.value

        when (status) {
            com.mikatechnology.BusTracker.services.LocationAuthStatus.NotDetermined -> {
                // Henüz sorulmadı, sistem diyaloğu göster
                if (isRequestingLocation) return false
                isRequestingLocation = true
                _activeSheet.value = null
                DriverStartPermissionPrefs.markFineLocationRequested(context)
                requestLocation()
                return false
            }
            com.mikatechnology.BusTracker.services.LocationAuthStatus.Denied -> {
                // Reddedildi, sheet göster
                _activeSheet.value = PassengerActionPermissionSheet.LocationForeground
                return false
            }
            else -> {
                // WhenInUse veya Always - geç
                if (_activeSheet.value == PassengerActionPermissionSheet.LocationForeground) {
                    _activeSheet.value = null
                }
                return true
            }
        }
    }

    /**
     * Hareket izni kontrolü (iOS parity):
     * - İzin varsa: true döner, akış devam
     * - notDetermined: sistem diyaloğu göster, false döner (callback'te devam)
     * - denied: sheet göster, false döner
     */
    private fun passMotionGate(context: Context, requestMotion: () -> Unit): Boolean {
        if (pendingGatedAction == null) return false

        // Android Q altında motion izni gerekmez
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true

        MotionActivityService.refreshAuthorization(context)

        // İzin zaten varsa geç
        if (MotionActivityService.hasActivityRecognitionPermission(context)) {
            didRequestMotionForAction = false
            if (_activeSheet.value == PassengerActionPermissionSheet.Motion) {
                _activeSheet.value = null
            }
            return true
        }

        // Henüz bu aksiyon için motion izni istenmedi mi?
        if (!didRequestMotionForAction) {
            // Sistem diyaloğu göster
            didRequestMotionForAction = true
            if (isRequestingMotion) return false
            isRequestingMotion = true
            _activeSheet.value = null
            requestMotion()
            return false
        }

        // Daha önce istendi ve reddedildi, sheet göster
        _activeSheet.value = PassengerActionPermissionSheet.Motion
        return false
    }

    companion object {
        /**
         * Harita sekmesi açılınca konum izni (iOS parity):
         * - notDetermined: sistem diyaloğu göster
         * - denied/granted: bir şey yapma
         */
        fun promptMapTabLocationIfNeeded(context: Context, requestFineLocation: () -> Unit) {
            LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)

            if (LocationTracker.hasFineLocation(context)) {
                LocationTracker.requestSingleLocation(context)
                return
            }

            val status = LocationTracker.authorizationStatus.value
            if (status == com.mikatechnology.BusTracker.services.LocationAuthStatus.NotDetermined) {
                DriverStartPermissionPrefs.markFineLocationRequested(context)
                requestFineLocation()
            }
            // denied ise haritada bottom sheet göstermiyoruz (iOS ile aynı)
            // aksiyon (biniş kaydet) tetiklenince sheet gösterilecek
        }
    }
}
