package com.mikatechnology.BusTracker.ui.driver

import android.content.Context
import android.os.Build
import com.mikatechnology.BusTracker.services.DriverStartPermissionPrefs
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.services.MotionActivityService

enum class DriverStartPermissionSheet {
    LocationForeground,
    LocationAlways,
    Motion
}

enum class StartTripPermissionGate {
    LocationForeground,
    LocationAlways,
    Motion,
    Ready
}

class DriverStartPermissionScope(
    val context: Context,
    val viewModel: DriverHomeViewModel,
    val requestForegroundLocation: () -> Unit,
    val requestActivityRecognition: () -> Unit,
    val pendingTripAfterPermissions: () -> Boolean,
    val setPendingTripAfterPermissions: (Boolean) -> Unit,
    val activeStartPermissionSheet: () -> DriverStartPermissionSheet?,
    val setActiveStartPermissionSheet: (DriverStartPermissionSheet?) -> Unit,
    val waitingForSettingsReturn: () -> Boolean,
    val setWaitingForSettingsReturn: (Boolean) -> Unit,
    val waitingForMotionSettingsReturn: () -> Boolean,
    val setWaitingForMotionSettingsReturn: (Boolean) -> Unit,
    val isRequestingFineLocation: () -> Boolean,
    val setIsRequestingFineLocation: (Boolean) -> Unit,
    val isRequestingMotion: () -> Boolean,
    val setIsRequestingMotion: (Boolean) -> Unit
) {
    fun refreshDriverLocationAuth() {
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
    }

    fun hasDriverMotionPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        MotionActivityService.refreshAuthorization(context)
        return MotionActivityService.hasActivityRecognitionPermission(context)
    }

    fun needsDriverAlwaysLocationUpgrade(): Boolean {
        refreshDriverLocationAuth()
        return LocationTracker.hasFineLocation(context) &&
            !LocationTracker.hasDriverAlwaysLocation(context)
    }

    fun presentDriverAlwaysLocationSheetIfNeeded(): Boolean {
        if (!needsDriverAlwaysLocationUpgrade()) return false
        setPendingTripAfterPermissions(true)
        setActiveStartPermissionSheet(DriverStartPermissionSheet.LocationAlways)
        setWaitingForSettingsReturn(false)
        return true
    }

    fun evaluateStartTripPermissionGate(): StartTripPermissionGate {
        refreshDriverLocationAuth()
        MotionActivityService.refreshAuthorization(context)
        if (!LocationTracker.hasFineLocation(context)) {
            return StartTripPermissionGate.LocationForeground
        }
        if (!LocationTracker.hasDriverAlwaysLocation(context)) {
            return StartTripPermissionGate.LocationAlways
        }
        if (!hasDriverMotionPermission()) {
            return StartTripPermissionGate.Motion
        }
        return StartTripPermissionGate.Ready
    }

    fun presentTripDurationSheet() {
        setActiveStartPermissionSheet(null)
        setPendingTripAfterPermissions(false)
        viewModel.presentTripDurationSheet()
    }

    fun presentMotionStep() {
        setWaitingForMotionSettingsReturn(false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            MotionActivityService.hasActivityRecognitionPermission(context)
        ) {
            presentTripDurationSheet()
            return
        }
        if (!DriverStartPermissionPrefs.hasRequestedActivityRecognition(context)) {
            setActiveStartPermissionSheet(null)
            DriverStartPermissionPrefs.markActivityRecognitionRequested(context)
            setIsRequestingMotion(true)
            requestActivityRecognition()
        } else {
            setActiveStartPermissionSheet(DriverStartPermissionSheet.Motion)
        }
    }

    fun presentStartTripPermissionGate(gate: StartTripPermissionGate) {
        when (gate) {
            StartTripPermissionGate.LocationForeground -> {
                setPendingTripAfterPermissions(true)
                setWaitingForSettingsReturn(false)
                refreshDriverLocationAuth()
                if (!LocationTracker.hasFineLocation(context)) {
                    if (!DriverStartPermissionPrefs.hasRequestedFineLocation(context)) {
                        setActiveStartPermissionSheet(null)
                        DriverStartPermissionPrefs.markFineLocationRequested(context)
                        setIsRequestingFineLocation(true)
                        requestForegroundLocation()
                    } else {
                        setActiveStartPermissionSheet(DriverStartPermissionSheet.LocationForeground)
                    }
                } else {
                    presentStartTripPermissionGate(StartTripPermissionGate.LocationAlways)
                }
            }
            StartTripPermissionGate.LocationAlways -> {
                setPendingTripAfterPermissions(true)
                setActiveStartPermissionSheet(DriverStartPermissionSheet.LocationAlways)
                setWaitingForSettingsReturn(false)
            }
            StartTripPermissionGate.Motion -> {
                setPendingTripAfterPermissions(true)
                presentMotionStep()
            }
            StartTripPermissionGate.Ready -> {
                presentTripDurationSheet()
            }
        }
    }

    fun continueStartTripPermissionFlow() {
        if (presentDriverAlwaysLocationSheetIfNeeded()) return
        when (val gate = evaluateStartTripPermissionGate()) {
            StartTripPermissionGate.Motion -> presentMotionStep()
            else -> presentStartTripPermissionGate(gate)
        }
    }

    fun requestForegroundLocationPermission() {
        setPendingTripAfterPermissions(false)
        if (LocationTracker.hasFineLocation(context)) return
        if (!DriverStartPermissionPrefs.hasRequestedFineLocation(context)) {
            setActiveStartPermissionSheet(null)
            DriverStartPermissionPrefs.markFineLocationRequested(context)
            setIsRequestingFineLocation(true)
            requestForegroundLocation()
        } else {
            setActiveStartPermissionSheet(DriverStartPermissionSheet.LocationForeground)
        }
    }

    fun onDriverPermissionsUpdated() {
        if (activeStartPermissionSheet() == DriverStartPermissionSheet.LocationForeground &&
            waitingForSettingsReturn()
        ) {
            setWaitingForSettingsReturn(false)
        }
        if (activeStartPermissionSheet() == DriverStartPermissionSheet.LocationAlways &&
            waitingForSettingsReturn()
        ) {
            setWaitingForSettingsReturn(false)
        }
        if (activeStartPermissionSheet() == DriverStartPermissionSheet.Motion &&
            waitingForMotionSettingsReturn()
        ) {
            setWaitingForMotionSettingsReturn(false)
        }
        refreshDriverLocationAuth()
        MotionActivityService.refreshAuthorization(context)
        if (LocationTracker.hasFineLocation(context)) {
            LocationTracker.requestSingleLocation(context)
        }
        if (!pendingTripAfterPermissions()) {
            if (LocationTracker.hasFineLocation(context) &&
                activeStartPermissionSheet() == DriverStartPermissionSheet.LocationForeground
            ) {
                setActiveStartPermissionSheet(null)
            }
            if (LocationTracker.hasDriverAlwaysLocation(context)) {
                setWaitingForSettingsReturn(false)
                if (activeStartPermissionSheet() == DriverStartPermissionSheet.LocationAlways) {
                    setActiveStartPermissionSheet(null)
                }
            }
            if (hasDriverMotionPermission()) {
                setWaitingForMotionSettingsReturn(false)
            }
            return
        }
        if (isRequestingFineLocation() || isRequestingMotion()) return
        if (presentDriverAlwaysLocationSheetIfNeeded()) return
        continueStartTripPermissionFlow()
    }

    fun canDriverStartTripNow(): Boolean {
        refreshDriverLocationAuth()
        return LocationTracker.driverHasAlwaysLocationForTrip(context)
    }

    fun canDriverStartTripFully(): Boolean {
        return canDriverStartTripNow() && hasDriverMotionPermission()
    }
}
