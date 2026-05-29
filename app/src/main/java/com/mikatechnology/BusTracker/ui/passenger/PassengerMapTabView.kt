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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.model.LatLng
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.ui.map.NeonMapOverlay
import com.mikatechnology.BusTracker.ui.map.ShuttleMapCamera
import com.mikatechnology.BusTracker.ui.map.ShuttleMapView
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

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
    var pendingCenterOnPassenger by remember { mutableStateOf(false) }
    var hasInitialCentered by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        LocationTracker.refreshAuthorizationStatus(context)
        if (granted) {
            pendingCenterOnPassenger = true
            LocationTracker.requestSingleLocation(context)
        }
    }

    fun requestPassengerLocation() {
        LocationTracker.refreshAuthorizationStatus(context)
        if (LocationTracker.hasFineLocation(context)) {
            pendingCenterOnPassenger = true
            LocationTracker.requestSingleLocation(context)
            deviceLocation?.let { loc ->
                val latLng = LatLng(loc.latitude, loc.longitude)
                onMapClick(latLng)
                scope.launch { mapCamera?.centerOn(latLng, animated = true) }
                pendingCenterOnPassenger = false
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
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

    LaunchedEffect(mapCamera) {
        if (hasInitialCentered || mapCamera == null) return@LaunchedEffect
        hasInitialCentered = true
        requestPassengerLocation()
    }

    LaunchedEffect(deviceLocation, pendingCenterOnPassenger) {
        if (!pendingCenterOnPassenger) return@LaunchedEffect
        val loc = deviceLocation ?: return@LaunchedEffect
        pendingCenterOnPassenger = false
        val latLng = LatLng(loc.latitude, loc.longitude)
        onMapClick(latLng)
        scope.launch { mapCamera?.centerOn(latLng, animated = true) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ShuttleMapView(
            driverLocation = driverLocation,
            driverRoute = driverRoute,
            isTripActive = isTripActive,
            morningPickups = morningPickupsForMap,
            selectedCoordinate = draftCoordinate,
            autoFitCameraOnUpdate = false,
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
                    isTripActive = isTripActive,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(0.1f))

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
                        icon = Icons.Default.MyLocation,
                        highlighted = true,
                        onClick = { requestPassengerLocation() }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonTheme.SurfaceContainer.copy(alpha = 0.82f))
                    .border(
                        width = 1.dp,
                        color = NeonTheme.Secondary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sabah biniş noktanız",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    color = NeonTheme.OnSurfaceVariant
                )
                Text(
                    text = "Haritaya dokunarak konum seçin",
                    fontSize = 12.sp,
                    color = NeonTheme.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Button(
                    onClick = onSavePickup,
                    enabled = !isSaving && draftCoordinate != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonTheme.Secondary,
                        contentColor = NeonTheme.OnSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSaving) "Kaydediliyor..." else "BİNİŞ NOKTASINI KAYDET",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
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

    Column(
        modifier = modifier
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = NeonTheme.Outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isTripActive) {
            Text(
                text = "Sürücü canlı konum",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = NeonTheme.OnSurfaceVariant
            )
            when {
                driverLocation != null -> {
                    Text(
                        text = driverLocation.driverName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonTheme.Secondary,
                        maxLines = 1
                    )
                    Text(
                        text = timeFormat.format(driverLocation.updatedAt),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonTheme.OnSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = "Konum bekleniyor",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonTheme.OnSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = groupName.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTheme.OnSurface,
                    maxLines = 1
                )
            }
            Text(
                text = "Servis bekliyor",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (highlighted) {
        NeonTheme.Secondary.copy(alpha = 0.2f)
    } else {
        NeonTheme.SurfaceContainerHigh.copy(alpha = 0.9f)
    }
    val border = if (highlighted) {
        NeonTheme.Secondary.copy(alpha = 0.5f)
    } else {
        NeonTheme.Outline.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .shadow(4.dp, spotColor = Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) NeonTheme.Secondary else NeonTheme.OnSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}
