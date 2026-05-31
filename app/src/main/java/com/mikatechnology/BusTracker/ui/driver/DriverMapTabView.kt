package com.mikatechnology.BusTracker.ui.driver

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.ui.map.NeonMapOverlay
import com.mikatechnology.BusTracker.ui.map.ShuttleMapCamera
import com.mikatechnology.BusTracker.ui.map.ShuttleMapView
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val WarningColor = androidx.compose.ui.graphics.Color(0xFFFFE04A)

@Composable
fun DriverMapTabView(
    driverLocation: DriverLocation?,
    driverRoute: List<LatLng> = emptyList(),
    morningPickups: List<MorningPickup>,
    stats: DriverPassengerStats,
    isTripActive: Boolean,
    modifier: Modifier = Modifier
) {
    var mapCamera by remember { mutableStateOf<ShuttleMapCamera?>(null) }
    val scope = rememberCoroutineScope()
    val nextPickup = nearestMorningPickup(driverLocation, morningPickups)

    LaunchedEffect(driverLocation?.updatedAt, morningPickups.size, mapCamera) {
        mapCamera?.updateData(driverLocation, morningPickups)
        mapCamera?.fitCamera(animated = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ShuttleMapView(
            driverLocation = driverLocation,
            driverRoute = driverRoute,
            isTripActive = isTripActive,
            morningPickups = morningPickups,
            modifier = Modifier.fillMaxSize(),
            onCameraReady = { camera ->
                mapCamera = camera
            }
        )

        NeonMapOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                NextStopCard(
                    pickup = nextPickup,
                    driverLocation = driverLocation
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTripActive) {
                        ActiveBadge()
                    }
                    MapControls(
                        onZoomIn = {
                            scope.launch { mapCamera?.zoom(by = 0.7) }
                        },
                        onZoomOut = {
                            scope.launch { mapCamera?.zoom(by = 1.35) }
                        },
                        onFit = {
                            scope.launch { mapCamera?.fitCamera(animated = true) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoCard(
                    title = "KAPASİTE",
                    value = stats.capacityOccupied.toString(),
                    suffix = "/ ${stats.total}",
                    valueColor = NeonTheme.Primary,
                    footnote = if (stats.unknown > 0) "${stats.unknown} belirtmedi" else null,
                    modifier = Modifier.weight(1f)
                )
                BentoCard(
                    title = "DURAKLAR",
                    value = morningPickups.size.toString(),
                    suffix = null,
                    valueColor = NeonTheme.OnSurface,
                    footnote = if (isTripActive) null else "Servis bekliyor",
                    showClock = !isTripActive,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActiveBadge() {
    Row(
        modifier = Modifier
            .background(NeonTheme.SurfaceContainerLow.copy(alpha = 0.9f))
            .border(1.dp, NeonTheme.Secondary.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NeonTheme.Secondary)
                .shadow(4.dp, spotColor = NeonTheme.Secondary.copy(alpha = 0.8f))
        )
        Text(
            text = "AKTİF",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = NeonTheme.Secondary
        )
    }
}

@Composable
private fun NextStopCard(
    pickup: MorningPickup?,
    driverLocation: DriverLocation?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .widthIn(max = 200.dp)
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.85f))
            .border(1.dp, NeonTheme.Secondary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(NeonTheme.Secondary)
        )
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "SONRAKİ DURAK",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.OnSurfaceVariant
            )
            if (pickup != null) {
                Text(
                    text = pickup.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTheme.OnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    distanceToPickup(driverLocation, pickup)?.let { distance ->
                        Text(
                            text = distance,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonTheme.Secondary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Sabah biniş",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonTheme.OnSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Durak yok",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTheme.OnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Yolcu biniş noktası bekleniyor.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapControlButton(icon = Icons.Default.Add, highlighted = false, onClick = onZoomIn)
        MapControlButton(icon = Icons.Default.Remove, highlighted = false, onClick = onZoomOut)
        MapControlButton(icon = Icons.Default.MyLocation, highlighted = true, onClick = onFit)
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val accent = if (highlighted) NeonTheme.Secondary else NeonTheme.OnSurface
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.9f))
            .border(
                1.dp,
                if (highlighted) NeonTheme.Secondary.copy(alpha = 0.5f) else NeonTheme.Outline.copy(alpha = 0.3f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = if (highlighted) {
                Modifier.shadow(6.dp, spotColor = NeonTheme.Secondary.copy(alpha = 0.2f))
            } else {
                Modifier
            }
        )
    }
}

@Composable
private fun BentoCard(
    title: String,
    value: String,
    suffix: String?,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    showClock: Boolean = false
) {
    Column(
        modifier = modifier
            .height(96.dp)
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.9f))
            .border(1.dp, NeonTheme.Outline.copy(alpha = 0.2f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = NeonTheme.OnSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = if (valueColor == NeonTheme.Primary) {
                    Modifier.shadow(6.dp, spotColor = NeonTheme.Primary.copy(alpha = 0.5f))
                } else {
                    Modifier
                }
            )
            suffix?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
        }
        footnote?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showClock) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = WarningColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurface
                )
            }
        }
    }
}

/** Firestore sırası değil — sürücüye en yakın geçerli biniş noktası. */
private fun nearestMorningPickup(
    driverLocation: DriverLocation?,
    pickups: List<MorningPickup>
): MorningPickup? {
    val valid = pickups.filter { isValidPickupCoordinate(it.latitude, it.longitude) }
    if (valid.isEmpty()) return pickups.firstOrNull()
    val driver = driverLocation ?: return valid.firstOrNull()
    val driverLoc = Location("driver").apply {
        latitude = driver.latitude
        longitude = driver.longitude
    }
    return valid.minByOrNull { pickup ->
        val stop = Location("stop").apply {
            latitude = pickup.latitude
            longitude = pickup.longitude
        }
        driverLoc.distanceTo(stop)
    }
}

private fun isValidPickupCoordinate(latitude: Double, longitude: Double): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    if (kotlin.math.abs(latitude) > 90 || kotlin.math.abs(longitude) > 180) return false
    if (kotlin.math.abs(latitude) <= 0.01 && kotlin.math.abs(longitude) <= 0.01) return false
    return true
}

private fun distanceToPickup(driverLocation: DriverLocation?, pickup: MorningPickup): String? {
    if (driverLocation == null) return null
    val results = FloatArray(1)
    Location.distanceBetween(
        driverLocation.latitude,
        driverLocation.longitude,
        pickup.latitude,
        pickup.longitude,
        results
    )
    val meters = results[0]
    return if (meters >= 1000) {
        String.format(java.util.Locale.US, "%.1f KM", meters / 1000)
    } else {
        "${meters.roundToInt()} M"
    }
}
