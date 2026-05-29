package com.mikatechnology.BusTracker.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.graphics.Color
import com.mikatechnology.BusTracker.BuildConfig
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MapDefaults
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.delay

private const val TAG = "ShuttleMapView"

@Composable
fun ShuttleMapView(
    driverLocation: DriverLocation?,
    driverRoute: List<LatLng> = emptyList(),
    isTripActive: Boolean = false,
    morningPickups: List<MorningPickup>,
    modifier: Modifier = Modifier,
    onCameraReady: (ShuttleMapCamera) -> Unit = {},
    onMapClick: ((LatLng) -> Unit)? = null,
    selectedCoordinate: LatLng? = null,
    autoFitCameraOnUpdate: Boolean = true
) {
    if (BuildConfig.MAPS_API_KEY.isBlank()) {
        MapApiKeyMissingBanner(modifier = modifier)
        return
    }

    val cameraPositionState = rememberCameraPositionState {
        position = MapDefaults.homeCameraPosition
    }

    var isMapLoaded by remember { mutableStateOf(false) }

    val camera = remember(cameraPositionState) {
        ShuttleMapCamera(cameraPositionState = cameraPositionState)
    }

    LaunchedEffect(camera) {
        onCameraReady(camera)
    }

    LaunchedEffect(
        isMapLoaded,
        driverLocation?.updatedAt,
        morningPickups.size,
        selectedCoordinate?.latitude,
        selectedCoordinate?.longitude
    ) {
        if (!isMapLoaded) return@LaunchedEffect
        camera.updateData(
            driverLocation = driverLocation,
            morningPickups = morningPickups,
            extraCoordinates = listOfNotNull(selectedCoordinate)
        )
        if (autoFitCameraOnUpdate) {
            camera.fitCamera(animated = false)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isBuildingEnabled = true,
                isTrafficEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false,
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true
            ),
            onMapLoaded = {
                isMapLoaded = true
                Log.d(TAG, "Map loaded")
            },
            onMapClick = onMapClick
        ) {
            MapEffect(driverLocation, morningPickups) { map ->
                map.setOnMapLoadedCallback {
                    Log.d(TAG, "Map tiles callback")
                }
            }

            if (DriverRouteDisplay.shouldDrawPolyline(driverRoute, driverLocation, isTripActive)) {
                Polyline(
                    points = DriverRouteDisplay.polylinePoints(driverRoute, driverLocation, isTripActive),
                    color = Color(0xBF00FFCC),
                    width = 10f,
                    geodesic = true
                )
            }

            driverLocation?.let { location ->
                // Custom driver marker - matches iOS "location.north.fill" arrow style with neon glow
                MarkerComposable(
                    state = MarkerState(LatLng(location.latitude, location.longitude)),
                    title = location.driverName,
                    snippet = "Sürücü"
                ) {
                    DriverMarkerView(driverName = location.driverName)
                }
            }

            val hasDraftAtNewLocation = selectedCoordinate != null &&
                morningPickups.none { pickup ->
                    LatLng(pickup.latitude, pickup.longitude).isSameLocationAs(selectedCoordinate)
                }

            morningPickups.forEach { pickup ->
                val pickupLatLng = LatLng(pickup.latitude, pickup.longitude)
                if (selectedCoordinate != null && pickupLatLng.isSameLocationAs(selectedCoordinate)) {
                    return@forEach
                }
                val style = if (hasDraftAtNewLocation) {
                    PickupMarkerStyle.SavedFaded
                } else {
                    PickupMarkerStyle.Saved
                }
                MarkerComposable(
                    state = MarkerState(pickupLatLng),
                    zIndex = if (style == PickupMarkerStyle.SavedFaded) 0.5f else 1f
                ) {
                    PickupMarkerView(style = style)
                }
            }

            selectedCoordinate?.let { coord ->
                MarkerComposable(
                    state = MarkerState(coord),
                    zIndex = 2f
                ) {
                    PickupMarkerView(style = PickupMarkerStyle.Draft)
                }
            }
        }
    }
}

@Composable
private fun MapApiKeyMissingBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeonTheme.SurfaceBright),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Harita yüklenemedi.\nlocal.properties dosyasına MAPS_API_KEY ekleyin\nveya Google Cloud'da Maps SDK for Android'i etkinleştirin.",
            color = NeonTheme.OnSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}



class ShuttleMapCamera(
    private val cameraPositionState: com.google.maps.android.compose.CameraPositionState
) {
    private var driverLocation: DriverLocation? = null
    private var morningPickups: List<MorningPickup> = emptyList()
    private var extraCoordinates: List<LatLng> = emptyList()

    fun updateData(
        driverLocation: DriverLocation?,
        morningPickups: List<MorningPickup>,
        extraCoordinates: List<LatLng> = emptyList()
    ) {
        this.driverLocation = driverLocation
        this.morningPickups = morningPickups
        this.extraCoordinates = extraCoordinates
    }

    suspend fun fitCamera(animated: Boolean = true) {
        val coordinates = buildList {
            addAll(morningPickups.map { LatLng(it.latitude, it.longitude) })
            addAll(extraCoordinates)
            driverLocation?.let { add(LatLng(it.latitude, it.longitude)) }
        }

        if (coordinates.isEmpty()) {
            moveCamera(CameraUpdateFactory.newCameraPosition(MapDefaults.homeCameraPosition), animated)
            return
        }

        if (coordinates.size == 1) {
            val target = coordinates.first()
            moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(target, 15f)
                ),
                animated
            )
            return
        }

        val builder = LatLngBounds.builder()
        coordinates.forEach(builder::include)
        moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 140), animated)
    }

    suspend fun zoom(by: Double) {
        val zoomDelta = if (by < 1.0) 1f else -1f
        val newZoom = (cameraPositionState.position.zoom + zoomDelta).coerceIn(10f, 20f)
        moveCamera(CameraUpdateFactory.zoomTo(newZoom), animated = true)
    }

    suspend fun centerOn(coordinate: LatLng, zoom: Float = 15f, animated: Boolean = true) {
        moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(coordinate, zoom)
            ),
            animated
        )
    }

    private suspend fun moveCamera(
        update: com.google.android.gms.maps.CameraUpdate,
        animated: Boolean
    ) {
        if (animated) {
            cameraPositionState.animate(update)
        } else {
            cameraPositionState.move(update)
        }
    }
}

private enum class PickupMarkerStyle {
    Saved,
    SavedFaded,
    Draft
}

private fun LatLng.isSameLocationAs(other: LatLng, epsilon: Double = 1e-5): Boolean {
    return kotlin.math.abs(latitude - other.latitude) < epsilon &&
        kotlin.math.abs(longitude - other.longitude) < epsilon
}

@Composable
private fun PickupMarkerView(
    style: PickupMarkerStyle,
    modifier: Modifier = Modifier
) {
    val pinColor = when (style) {
        PickupMarkerStyle.Saved -> NeonTheme.MapPickupPin
        PickupMarkerStyle.SavedFaded -> NeonTheme.MapPickupPin.copy(alpha = 0.45f)
        PickupMarkerStyle.Draft -> NeonTheme.MapPickupPin
    }
    val iconSize = when (style) {
        PickupMarkerStyle.Draft -> 36.dp
        PickupMarkerStyle.Saved -> 32.dp
        PickupMarkerStyle.SavedFaded -> 32.dp
    }
    val haloAlpha = when (style) {
        PickupMarkerStyle.Draft -> 0.08f
        PickupMarkerStyle.Saved -> 0.22f
        PickupMarkerStyle.SavedFaded -> 0.1f
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize + 8.dp)
                .clip(CircleShape)
                .background(pinColor.copy(alpha = haloAlpha))
        )
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = pinColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Custom driver location marker that mimics the iOS style:
 * Large glowing circle + directional arrow (instead of default pin).
 */
@Composable
fun DriverMarkerView(driverName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NeonTheme.MapDriverPin.copy(alpha = 0.15f))
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = NeonTheme.MapDriverPin.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            )

            Icon(
                imageVector = Icons.Filled.Navigation,
                contentDescription = null,
                tint = NeonTheme.MapDriverPin,
                modifier = Modifier
                    .size(28.dp)
                    .shadow(
                        elevation = 8.dp,
                        spotColor = NeonTheme.MapDriverPin.copy(alpha = 0.8f),
                        ambientColor = NeonTheme.MapDriverPin.copy(alpha = 0.4f)
                    )
            )
        }

        if (driverName.isNotBlank()) {
            Text(
                text = driverName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.OnSurface,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonTheme.SurfaceContainer.copy(alpha = 0.92f))
                    .border(
                        width = 1.dp,
                        color = NeonTheme.MapDriverPin.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
