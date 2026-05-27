package com.mikatechnology.BusTracker.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
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
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
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
    morningPickups: List<MorningPickup>,
    modifier: Modifier = Modifier,
    onCameraReady: (ShuttleMapCamera) -> Unit = {},
    onMapClick: ((LatLng) -> Unit)? = null,
    selectedCoordinate: LatLng? = null,
    selectedCoordinateTitle: String = "Seçilen Biniş Noktası"
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
        ShuttleMapCamera(
            cameraPositionState = cameraPositionState,
            driverLocation = driverLocation,
            morningPickups = morningPickups
        )
    }

    LaunchedEffect(cameraPositionState) {
        onCameraReady(camera)
    }



    LaunchedEffect(isMapLoaded, driverLocation?.updatedAt, morningPickups.size) {
        if (!isMapLoaded) return@LaunchedEffect
        camera.updateData(driverLocation, morningPickups)
        camera.fitCamera(animated = false)
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

            morningPickups.forEach { pickup ->
                Marker(
                    state = MarkerState(LatLng(pickup.latitude, pickup.longitude)),
                    title = pickup.name,
                    snippet = "Kayıtlı biniş"
                )
            }

            // Draft / currently selected coordinate (for passenger choosing pickup point)
            selectedCoordinate?.let { coord ->
                Marker(
                    state = MarkerState(coord),
                    title = selectedCoordinateTitle,
                    snippet = "Dokunarak değiştir"
                )
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
    private val cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    private var driverLocation: DriverLocation?,
    private var morningPickups: List<MorningPickup>
) {
    suspend fun fitCamera(animated: Boolean = true) {
        val coordinates = buildList {
            addAll(morningPickups.map { LatLng(it.latitude, it.longitude) })
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

    fun updateData(driverLocation: DriverLocation?, morningPickups: List<MorningPickup>) {
        this.driverLocation = driverLocation
        this.morningPickups = morningPickups
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

/**
 * Custom driver location marker that mimics the iOS style:
 * Large glowing circle + directional arrow (instead of default pin).
 */
@Composable
fun DriverMarkerView(driverName: String) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(NeonTheme.Secondary.copy(alpha = 0.15f))
        )

        // Ring
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = NeonTheme.Secondary.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        )

        // Directional arrow (this is what the user sees as "ok" / arrow on iOS)
        Icon(
            imageVector = Icons.Filled.Navigation,
            contentDescription = null,
            tint = NeonTheme.Secondary,
            modifier = Modifier
                .size(28.dp)
                .shadow(
                    elevation = 8.dp,
                    spotColor = NeonTheme.Secondary.copy(alpha = 0.8f),
                    ambientColor = NeonTheme.Secondary.copy(alpha = 0.4f)
                )
        )
    }
}
