package com.mikatechnology.BusTracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.Process
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

/** Who is requesting location — background/"always" applies to drivers only. */
enum class LocationPermissionRole {
    Driver,
    Passenger
}

enum class LocationAuthStatus {
    NotDetermined,
    Denied,
    WhenInUse,
    Always;

    val isDenied: Boolean
        get() = this == Denied

    /** Sürücü sefer paylaşımı: "Uygulama kullanılırken" yetmez, "Her zaman" gerekir. */
    fun needsAlwaysAuthorization(role: LocationPermissionRole): Boolean =
        role == LocationPermissionRole.Driver && this == WhenInUse

    val isAuthorizedForTracking: Boolean
        get() = this == WhenInUse || this == Always

    fun isAuthorizedForRole(role: LocationPermissionRole): Boolean = when (role) {
        LocationPermissionRole.Driver -> isAuthorizedForTracking
        LocationPermissionRole.Passenger -> this == WhenInUse || this == Always
    }
}

object LocationTracker {
    /** [AppOpsManager.OPSTR_BACKGROUND_LOCATION] — API 29+ sabit adı bazı derlemelerde görünmeyebilir. */
    private const val BACKGROUND_LOCATION_APP_OP = "android:background_location"

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _authorizationStatus = MutableStateFlow(LocationAuthStatus.NotDetermined)
    val authorizationStatus: StateFlow<LocationAuthStatus> = _authorizationStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var appContext: Context? = null
    private var fusedClient: com.google.android.gms.location.FusedLocationProviderClient? = null
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
            refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
            return
        }
        appContext = context.applicationContext
        fusedClient = LocationServices.getFusedLocationProviderClient(appContext!!)
        refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
        if (BuildConfig.DEBUG) {
            _currentLocation.value = MapDefaults.homeLocation
        }
    }

    fun refreshAuthorizationStatus(
        context: Context,
        role: LocationPermissionRole = LocationPermissionRole.Driver
    ) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val foregroundGranted = fineGranted || coarseGranted
        val backgroundGranted = role == LocationPermissionRole.Driver &&
            hasBackgroundLocationAccess(context)

        _authorizationStatus.value = when {
            role == LocationPermissionRole.Driver &&
                foregroundGranted &&
                backgroundGranted -> LocationAuthStatus.Always
            foregroundGranted -> LocationAuthStatus.WhenInUse
            else -> LocationAuthStatus.Denied
        }
    }

    /**
     * Ayarlardan "Her zaman" seçildiğinde [ACCESS_BACKGROUND_LOCATION] bazen gecikmeli
     * güncellenir; AppOps ile birlikte kontrol edilir.
     */
    private fun hasBackgroundLocationAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true
        }
        val appContext = context.applicationContext
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return try {
            val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                appOps.unsafeCheckOpNoThrow(
                    BACKGROUND_LOCATION_APP_OP,
                    Process.myUid(),
                    appContext.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    BACKGROUND_LOCATION_APP_OP,
                    Process.myUid(),
                    appContext.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
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
        val client = fusedClient ?: return
        client.lastLocation.addOnSuccessListener { location ->
            location?.let(::deliverLocation)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (appContext == null) return
        if (BuildConfig.DEBUG) {
            isTracking = true
            deliverLocation(MapDefaults.homeLocation)
            return
        }

        if (!_authorizationStatus.value.isAuthorizedForTracking) return
        val client = fusedClient ?: return

        isTracking = true
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        client.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        isTracking = false
        onLocationUpdate = null
        if (BuildConfig.DEBUG) return
        fusedClient?.removeLocationUpdates(locationCallback)
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

    fun hasDriverAlwaysLocation(context: Context): Boolean {
        refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
        return _authorizationStatus.value == LocationAuthStatus.Always
    }

    /** Sürücü seferi — yalnızca "Her zaman" konum izni ile başlatılabilir. */
    fun canDriverStartTrip(): Boolean {
        val ctx = appContext ?: return false
        return hasDriverAlwaysLocation(ctx)
    }

    /**
     * Convenience for passengers who only need when-in-use permission
     * (to show their own location on the map).
     */
    fun requestWhenInUse(context: Context) {
        refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
        if (hasFineLocation(context)) {
            requestSingleLocation(context)
        }
    }

    fun hasWhenInUseLocation(context: Context): Boolean {
        refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
        return _authorizationStatus.value.isAuthorizedForRole(LocationPermissionRole.Passenger)
    }
}
