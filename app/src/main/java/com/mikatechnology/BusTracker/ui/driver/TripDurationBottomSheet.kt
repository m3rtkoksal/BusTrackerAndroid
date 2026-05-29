package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private val durationOptions = listOf(1.0, 1.5, 2.0, 3.0, 4.0)

@Composable
private fun DurationOptionChip(
    hours: Double,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (hours == hours.toLong().toDouble()) {
        "${hours.toInt()} saat"
    } else {
        "$hours saat"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) NeonTheme.Primary.copy(alpha = 0.15f)
                else NeonTheme.SurfaceBright.copy(alpha = 0.8f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) NeonTheme.Primary else NeonTheme.Outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) NeonTheme.Primary else NeonTheme.OnSurfaceVariant
        )
    }
}

@Composable
fun TripDurationBottomSheet(
    selectedHours: Double,
    onSelectedHoursChange: (Double) -> Unit,
    isLoading: Boolean,
    canStartTrip: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .background(NeonTheme.SurfaceContainer)
            .padding(horizontal = 28.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 20.dp)
                .size(width = 48.dp, height = 4.dp)
                .clip(CircleShape)
                .background(NeonTheme.SurfaceContainerHigh)
        )

        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = NeonTheme.Primary,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Servis Süresi",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = NeonTheme.OnSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (canStartTrip) {
                "Sefer boyunca konumunuz yolculara paylaşılır. Paylaşım süre sonunda otomatik durur."
            } else {
                "\"Her zaman\" konum izni olmadan servis başlatılamaz. Önce İZİN VER adımlarını tamamlayın."
            },
            fontSize = 14.sp,
            color = if (canStartTrip) NeonTheme.OnSurfaceVariant else NeonTheme.Error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            durationOptions.chunked(3).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowOptions.forEach { hours ->
                        DurationOptionChip(
                            hours = hours,
                            selected = selectedHours == hours,
                            onSelect = { onSelectedHoursChange(hours) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowOptions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonTheme.SurfaceContainerHigh)
                .border(1.dp, NeonTheme.Primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable(enabled = canStartTrip && !isLoading, onClick = onConfirm)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = NeonTheme.Primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "SERVİSİ BAŞLAT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = if (canStartTrip) NeonTheme.Primary else NeonTheme.OnSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
