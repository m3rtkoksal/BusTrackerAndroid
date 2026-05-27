package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.mikatechnology.BusTracker.data.model.DriverLocation
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.ui.map.NeonMapOverlay
import com.mikatechnology.BusTracker.ui.map.ShuttleMapView
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

/**
 * Passenger map tab.
 * Currently re-uses ShuttleMapView (driver-focused). 
 * Full tap-to-select + custom neon markers (matching iOS PassengerLiveMap) 
 * should be added in a follow-up by extending ShuttleMapView or creating PassengerMapView.
 */
@Composable
fun PassengerMapTabView(
    driverLocation: DriverLocation?,
    draftCoordinate: LatLng?,
    savedPickup: MorningPickup?,
    isTripActive: Boolean,
    isSaving: Boolean,
    onMapClick: (LatLng) -> Unit,
    onSavePickup: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Base map with tap support for selecting pickup point
        ShuttleMapView(
            driverLocation = driverLocation,
            morningPickups = savedPickup?.let { listOf(it) } ?: emptyList(),
            selectedCoordinate = draftCoordinate,
            selectedCoordinateTitle = "Seçilen Biniş Noktası",
            onMapClick = onMapClick,
            modifier = Modifier.fillMaxSize()
        )

        NeonMapOverlay()

        // Top info + controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Compact info card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonTheme.SurfaceContainer.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = NeonTheme.Secondary.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonTheme.Secondary)
                                .shadow(4.dp, spotColor = NeonTheme.Secondary.copy(alpha = 0.8f))
                        )
                        Text(
                            text = "SERVİS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTheme.OnSurface,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Text(
                        text = if (driverLocation != null) {
                            "${driverLocation.driverName.uppercase()} • aktif"
                        } else if (isTripActive) "Konum bekleniyor" else "Servis bekliyor",
                        fontSize = 10.sp,
                        color = if (driverLocation != null) NeonTheme.Secondary else NeonTheme.OnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // Zoom controls
                Column(horizontalAlignment = Alignment.End) {
                    MapControlButton(icon = Icons.Default.Add, onClick = onZoomIn)
                    MapControlButton(icon = Icons.Default.Remove, onClick = onZoomOut)
                    MapControlButton(
                        icon = Icons.Default.MyLocation,
                        highlighted = true,
                        onClick = onFitAll
                    )
                }
            }
        }

        // Bottom pickup action bar
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
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (draftCoordinate != null)
                        "Seçilen nokta haritada işaretli. Kaydetmek için butona bas."
                    else
                        "Haritaya dokunarak biniş noktanızı seçin.",
                    fontSize = 10.sp,
                    color = NeonTheme.OnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSavePickup,
                    enabled = draftCoordinate != null && !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (draftCoordinate != null)
                            NeonTheme.SurfaceContainerHigh
                        else
                            NeonTheme.SurfaceContainer,
                        contentColor = if (draftCoordinate != null)
                            NeonTheme.Secondary
                        else
                            NeonTheme.OnSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSaving) "Kaydediliyor..." else "BİNİŞ NOKTAMI KAYDET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) NeonTheme.Secondary else NeonTheme.OnSurface,
            modifier = Modifier
                .size(40.dp)
                .background(NeonTheme.SurfaceContainer.copy(alpha = 0.92f), CircleShape)
                .border(
                    width = 1.dp,
                    color = if (highlighted) NeonTheme.Secondary.copy(alpha = 0.5f) else NeonTheme.Outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .padding(8.dp)
        )
    }
}
