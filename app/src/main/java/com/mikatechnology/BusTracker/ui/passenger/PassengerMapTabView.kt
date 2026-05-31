package com.mikatechnology.BusTracker.ui.passenger

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.model.LatLng
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.google.maps.android.compose.MapType
import com.mikatechnology.BusTracker.ui.map.NeonMapOverlay
import com.mikatechnology.BusTracker.ui.map.ShuttleMapCamera
import com.mikatechnology.BusTracker.ui.map.ShuttleMapView
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

private enum class PassengerGpsFocus {
    Primary,
    Alternate
}

private val MapUiShape = RectangleShape

@Composable
fun PassengerMapTabView(
    groupName: String,
    driverLocation: DriverLocation?,
    driverRoute: List<LatLng>,
    draftCoordinate: LatLng?,
    savedPickup: MorningPickup?,
    isTripActive: Boolean,
    isSaving: Boolean,
    onMapClick: (LatLng) -> Unit,
    onSavePickup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapCamera by remember { mutableStateOf<ShuttleMapCamera?>(null) }
    val scope = rememberCoroutineScope()
    val deviceLocation by LocationTracker.currentLocation.collectAsState()
    /// false = bir sonraki basış birincil odak, true = bir sonraki basış alternatif odak.
    var mapFocusNextIsAlternate by remember { mutableStateOf(false) }
    var pendingGpsFocus by remember { mutableStateOf<PassengerGpsFocus?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
        if (granted) {
            LocationTracker.requestSingleLocation(context)
        }
    }

    fun centerOnCoordinate(latLng: LatLng) {
        scope.launch { mapCamera?.centerOn(latLng, animated = true) }
    }

    fun centerOnCurrentLocation(gpsFocus: PassengerGpsFocus) {
        pendingGpsFocus = gpsFocus
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
        if (LocationTracker.hasFineLocation(context)) {
            LocationTracker.requestSingleLocation(context)
            deviceLocation?.let { loc ->
                pendingGpsFocus = null
                val latLng = LatLng(loc.latitude, loc.longitude)
                if (gpsFocus == PassengerGpsFocus.Primary) {
                    onMapClick(latLng)
                }
                centerOnCoordinate(latLng)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun focusOnPrimaryMapTarget() {
        draftCoordinate?.let {
            centerOnCoordinate(it)
            return
        }
        centerOnCurrentLocation(PassengerGpsFocus.Primary)
    }

    fun focusOnAlternateMapTarget() {
        if (isTripActive && driverLocation != null) {
            centerOnCoordinate(LatLng(driverLocation.latitude, driverLocation.longitude))
            return
        }
        savedPickup?.let {
            centerOnCoordinate(LatLng(it.latitude, it.longitude))
            return
        }
        centerOnCurrentLocation(PassengerGpsFocus.Alternate)
    }

    fun togglePassengerMapFocus() {
        if (mapFocusNextIsAlternate) {
            focusOnAlternateMapTarget()
            mapFocusNextIsAlternate = false
        } else {
            focusOnPrimaryMapTarget()
            mapFocusNextIsAlternate = true
        }
    }

    val morningPickupsForMap = remember(savedPickup) {
        savedPickup?.let { listOf(it) } ?: emptyList()
    }

    LaunchedEffect(isTripActive) {
        if (!isTripActive) {
            mapFocusNextIsAlternate = false
        }
    }

    LaunchedEffect(driverLocation?.updatedAt, savedPickup, draftCoordinate, mapCamera) {
        mapCamera?.updateData(
            driverLocation = driverLocation,
            morningPickups = morningPickupsForMap,
            extraCoordinates = listOfNotNull(draftCoordinate)
        )
    }

    LaunchedEffect(deviceLocation, pendingGpsFocus) {
        val gpsFocus = pendingGpsFocus ?: return@LaunchedEffect
        val loc = deviceLocation ?: return@LaunchedEffect
        pendingGpsFocus = null
        val latLng = LatLng(loc.latitude, loc.longitude)
        if (gpsFocus == PassengerGpsFocus.Primary) {
            onMapClick(latLng)
        }
        centerOnCoordinate(latLng)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ShuttleMapView(
            driverLocation = driverLocation,
            driverRoute = driverRoute,
            isTripActive = isTripActive,
            morningPickups = morningPickupsForMap,
            selectedCoordinate = draftCoordinate,
            autoFitCameraOnUpdate = false,
            mapType = MapType.HYBRID,
            onMapClick = onMapClick,
            onCameraReady = { mapCamera = it },
            modifier = Modifier.fillMaxSize()
        )

        NeonMapOverlay()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                PassengerMapCompactInfo(
                    groupName = groupName,
                    driverLocation = driverLocation,
                    isTripActive = isTripActive
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    MapControlButton(
                        icon = Icons.Default.Add,
                        onClick = { scope.launch { mapCamera?.zoom(by = 0.7) } }
                    )
                    MapControlButton(
                        icon = Icons.Default.Remove,
                        onClick = { scope.launch { mapCamera?.zoom(by = 1.35) } }
                    )
                    MapControlButton(
                        icon = if (mapFocusNextIsAlternate) {
                            Icons.Default.MyLocation
                        } else {
                            Icons.Default.Navigation
                        },
                        accentStyle = true,
                        onClick = { togglePassengerMapFocus() }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MapUiShape)
                    .background(NeonTheme.SurfaceContainer.copy(alpha = 0.82f))
                    .background(Color.Black.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = NeonTheme.Secondary.copy(alpha = 0.22f),
                        shape = MapUiShape
                    )
                    .padding(horizontal = 12.dp)
                    .padding(top = 16.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Haritaya dokunarak biniş noktanızı seçin.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = NeonTheme.OnSurfaceVariant,
                    lineHeight = 12.sp
                )

                SavePickupButton(
                    isSaving = isSaving,
                    enabled = draftCoordinate != null,
                    onClick = onSavePickup
                )
            }
        }
    }
}

@Composable
private fun PassengerMapCompactInfo(
    groupName: String,
    driverLocation: DriverLocation?,
    isTripActive: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = modifier
            .widthIn(max = 200.dp)
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(NeonTheme.Secondary)
        )
        Column(
            modifier = Modifier
                .clip(MapUiShape)
                .background(NeonTheme.SurfaceContainer.copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    color = NeonTheme.Secondary.copy(alpha = 0.22f),
                    shape = MapUiShape
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isTripActive) {
                when {
                    driverLocation != null -> {
                        Text(
                            text = driverLocation.driverName.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTheme.Secondary,
                            maxLines = 1
                        )
                        Text(
                            text = timeFormat.format(driverLocation.updatedAt),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonTheme.OnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    else -> {
                        Text(
                            text = groupName.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTheme.OnSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Konum bekleniyor",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonTheme.OnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Text(
                    text = groupName.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTheme.OnSurface,
                    maxLines = 1
                )
                Text(
                    text = "Servis bekliyor",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SavePickupButton(
    isSaving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val canTap = enabled && !isSaving
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canTap) 1f else 0.45f)
            .clip(MapUiShape)
            .background(NeonTheme.SurfaceContainerHigh.copy(alpha = 0.9f))
            .border(
                width = 1.dp,
                color = NeonTheme.MapSaveAction.copy(alpha = 0.5f),
                shape = MapUiShape
            )
            .clickable(enabled = canTap) { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = NeonTheme.MapSaveAction,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = NeonTheme.MapSaveAction,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "BİNİŞ NOKTAMI KAYDET",
                style = TextStyle(
                    color = NeonTheme.MapSaveAction,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    shadow = Shadow(
                        color = NeonTheme.MapSaveAction.copy(alpha = 0.55f),
                        offset = Offset.Zero,
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    highlighted: Boolean = false,
    /** Koyu kutu + ince neon çerçeve ve ikon (konum tuşu, iOS gibi). */
    accentStyle: Boolean = false,
    onClick: () -> Unit
) {
    val useAccent = highlighted || accentStyle
    val bg = if (highlighted) {
        NeonTheme.Secondary.copy(alpha = 0.2f)
    } else {
        NeonTheme.SurfaceContainerHigh.copy(alpha = 0.9f)
    }
    val border = if (useAccent) {
        NeonTheme.Secondary.copy(alpha = 0.5f)
    } else {
        NeonTheme.Outline.copy(alpha = 0.3f)
    }
    val resolvedIconTint = if (useAccent) NeonTheme.Secondary else NeonTheme.OnSurface

    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .size(40.dp)
            .clip(MapUiShape)
            .background(bg)
            .border(1.dp, border, MapUiShape)
            .shadow(
                elevation = if (useAccent) 6.dp else 4.dp,
                spotColor = if (useAccent) {
                    NeonTheme.Secondary.copy(alpha = 0.2f)
                } else {
                    Color.Black.copy(alpha = 0.3f)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = resolvedIconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}
