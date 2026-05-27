package com.mikatechnology.BusTracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mikatechnology.BusTracker.BuildConfig
import com.mikatechnology.BusTracker.data.model.MapDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LocationAuthStatus {
    NotDetermined,
    Denied,
    WhenInUse,
    Always;

    val isDenied: Boolean
        get() = this == Denied

    val needsAlwaysAuthorization: Boolean
        get() = this == WhenInUse

    val isAuthorizedForTracking: Boolean
        get() = this == WhenInUse || this == Always
}

object LocationTracker {
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _authorizationStatus = MutableStateFlow(LocationAuthStatus.NotDetermined)
    val authorizationStatus: StateFlow<LocationAuthStatus> = _authorizationStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var appContext: Context? = null
    private lateinit var fusedClient: com.google.android.gms.location.FusedLocationProviderClient
    private var onLocationUpdate: ((Location) -> Unit)? = null
    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            deliverLocation(location)
        }
    }

    fun initialize(context: Context) {
        if (appContext != null) {
            refreshAuthorizationStatus(context)
            return
        }
        appContext = context.applicationContext
        fusedClient = LocationServices.getFusedLocationProviderClient(appContext!!)
        refreshAuthorizationStatus(context)
        if (BuildConfig.DEBUG) {
            _currentLocation.value = MapDefaults.homeLocation
        }
    }

    fun refreshAuthorizationStatus(context: Context) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val backgroundGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _authorizationStatus.value = when {
            fineGranted && backgroundGranted -> LocationAuthStatus.Always
            fineGranted || coarseGranted -> LocationAuthStatus.WhenInUse
            else -> LocationAuthStatus.Denied
        }
    }

    fun setOnLocationUpdate(listener: ((Location) -> Unit)?) {
        onLocationUpdate = listener
    }

    val effectiveLocation: Location?
        get() = if (BuildConfig.DEBUG) MapDefaults.homeLocation else _currentLocation.value

    @SuppressLint("MissingPermission")
    fun requestSingleLocation(context: Context) {
        initialize(context)
        if (!_authorizationStatus.value.isAuthorizedForTracking) return
        fusedClient.lastLocation.addOnSuccessListener { location ->
            location?.let(::deliverLocation)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        val context = appContext ?: return
        if (BuildConfig.DEBUG) {
            isTracking = true
            deliverLocation(MapDefaults.homeLocation)
            return
        }

        if (!_authorizationStatus.value.isAuthorizedForTracking) return

        isTracking = true
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        isTracking = false
        onLocationUpdate = null
        if (BuildConfig.DEBUG) return
        fusedClient.removeLocationUpdates(locationCallback)
    }

    private fun deliverLocation(location: Location) {
        val resolved = if (BuildConfig.DEBUG) MapDefaults.homeLocation else location
        _currentLocation.value = resolved
        onLocationUpdate?.invoke(resolved)
    }

    fun hasFineLocation(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Convenience for passengers who only need when-in-use permission
     * (to show their own location on the map).
     */
    fun requestWhenInUse(context: Context) {
        // In a real implementation this would trigger a permission request.
        // For now we just refresh status (DriverHomeView already has the launcher pattern).
        refreshAuthorizationStatus(context)
        if (hasFineLocation(context)) {
            requestSingleLocation(context)
        }
    }
}
