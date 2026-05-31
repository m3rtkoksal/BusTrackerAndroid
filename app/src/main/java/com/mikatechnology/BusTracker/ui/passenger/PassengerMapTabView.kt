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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
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
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
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
    Primary
}

private val MapUiShape = RectangleShape
private val AttendanceNotComingColor = Color(0xFFFF4444)
private val AttendanceUnknownColor = Color(0xFFFFE04A)

@Composable
fun PassengerMapTabView(
    groupName: String,
    driverLocation: DriverLocation?,
    driverRoute: List<LatLng>,
    draftCoordinate: LatLng?,
    savedPickup: MorningPickup?,
    myAttendance: AttendanceStatus,
    onAttendanceClick: () -> Unit,
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

    fun focusOnSavedPickupOrDevice() {
        savedPickup?.let {
            centerOnCoordinate(LatLng(it.latitude, it.longitude))
            return
        }
        centerOnCurrentLocation(PassengerGpsFocus.Primary)
    }

    fun centerMapOnPassengerPickup(camera: ShuttleMapCamera, animated: Boolean) {
        val pickup = draftCoordinate
            ?: savedPickup?.let { LatLng(it.latitude, it.longitude) }
        if (pickup != null) {
            scope.launch { camera.centerOn(pickup, zoom = 15f, animated = animated) }
            return
        }
        if (isTripActive) {
            driverLocation?.let { loc ->
                scope.launch {
                    camera.centerOn(LatLng(loc.latitude, loc.longitude), zoom = 15f, animated = animated)
                }
            }
        }
    }

    val showsDriverMapButton = isTripActive

    fun focusOnDriverLocation() {
        val loc = driverLocation ?: return
        centerOnCoordinate(LatLng(loc.latitude, loc.longitude))
    }

    val morningPickupsForMap = remember(savedPickup) {
        savedPickup?.let { listOf(it) } ?: emptyList()
    }

    LaunchedEffect(driverLocation?.updatedAt, savedPickup, draftCoordinate, mapCamera) {
        mapCamera?.updateData(
            driverLocation = driverLocation,
            morningPickups = morningPickupsForMap,
            extraCoordinates = listOfNotNull(draftCoordinate)
        )
    }

    LaunchedEffect(mapCamera, isTripActive, savedPickup?.memberID, savedPickup?.updatedAt) {
        val camera = mapCamera ?: return@LaunchedEffect
        kotlinx.coroutines.delay(80)
        centerMapOnPassengerPickup(camera, animated = false)
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.widthIn(max = 200.dp)
                ) {
                    PassengerMapCompactInfo(
                        groupName = groupName,
                        driverLocation = driverLocation,
                        isTripActive = isTripActive
                    )
                    PassengerMapAttendanceInfo(
                        attendance = myAttendance,
                        onClick = onAttendanceClick
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapControlButton(
                        icon = if (savedPickup != null) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        accentStyle = true,
                        onClick = { focusOnSavedPickupOrDevice() }
                    )
                    if (showsDriverMapButton) {
                        MapControlButton(
                            icon = Icons.Default.DirectionsBus,
                            accentStyle = true,
                            iconTint = NeonTheme.MapDriverPin,
                            onClick = { focusOnDriverLocation() }
                        )
                    }
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
    val accent = if (isTripActive) NeonTheme.Secondary else NeonTheme.Outline
    val titleColor = if (isTripActive) NeonTheme.OnSurface else NeonTheme.OnSurfaceVariant
    val boxAlpha = if (isTripActive) 0.72f else 0.58f
    val borderAlpha = if (isTripActive) 0.22f else 0.35f

    Row(
        modifier = modifier
            .widthIn(max = 200.dp)
            .height(IntrinsicSize.Min)
            .alpha(if (isTripActive) 1f else 0.88f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(accent)
        )
        Column(
            modifier = Modifier
                .clip(MapUiShape)
                .background(NeonTheme.SurfaceContainer.copy(alpha = boxAlpha))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = borderAlpha),
                    shape = MapUiShape
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accent)
                )
                Text(
                    text = groupName.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1
                )
            }

            if (isTripActive) {
                when {
                    driverLocation != null -> {
                        Text(
                            text = driverLocation.driverName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
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
                    text = "Servis pasif",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PassengerMapAttendanceInfo(
    attendance: AttendanceStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = attendanceMapAccent(attendance)

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(accent)
        )
        Row(
            modifier = Modifier
                .clip(MapUiShape)
                .background(accent.copy(alpha = 0.14f))
                .background(NeonTheme.SurfaceContainer.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.48f),
                    shape = MapUiShape
                )
                .shadow(
                    elevation = 2.dp,
                    shape = MapUiShape,
                    spotColor = accent.copy(alpha = 0.35f)
                )
                .padding(start = 8.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = attendance.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = attendance.mapTabLabel.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
            Text(
                text = "DEĞİŞTİR",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = NeonTheme.OnSurfaceVariant,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun attendanceMapAccent(status: AttendanceStatus): Color = when (status) {
    AttendanceStatus.Coming -> NeonTheme.Secondary
    AttendanceStatus.NotComing -> AttendanceNotComingColor
    AttendanceStatus.Unknown -> AttendanceUnknownColor
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
    compact: Boolean = false,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    val useAccent = highlighted || accentStyle
    val accentColor = iconTint ?: NeonTheme.Secondary
    val bg = if (highlighted) {
        accentColor.copy(alpha = 0.2f)
    } else {
        NeonTheme.SurfaceContainerHigh.copy(alpha = 0.9f)
    }
    val border = if (useAccent) {
        accentColor.copy(alpha = 0.55f)
    } else {
        NeonTheme.Outline.copy(alpha = 0.3f)
    }
    val resolvedIconTint = iconTint ?: if (useAccent) NeonTheme.Secondary else NeonTheme.OnSurface
    val buttonSize = if (compact) 32.dp else 40.dp
    val iconSize = if (compact) 14.dp else 18.dp
    val bottomPad = if (compact) 4.dp else 8.dp

    Box(
        modifier = Modifier
            .padding(bottom = bottomPad)
            .size(buttonSize)
            .clip(MapUiShape)
            .background(bg)
            .border(1.dp, border, MapUiShape)
            .shadow(
                elevation = if (useAccent) 6.dp else 4.dp,
                spotColor = if (useAccent) {
                    accentColor.copy(alpha = 0.25f)
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
            modifier = Modifier.size(iconSize)
        )
    }
}
