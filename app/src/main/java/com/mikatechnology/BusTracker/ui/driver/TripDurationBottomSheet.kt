package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun TripDurationBottomSheet(
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
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            tint = NeonTheme.Primary,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = L10n.tripStartConfirmTitle,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = NeonTheme.OnSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (canStartTrip) L10n.tripStartConfirmBody else L10n.tripDurationBodyNeedsPermission,
            fontSize = 14.sp,
            color = if (canStartTrip) NeonTheme.OnSurfaceVariant else NeonTheme.Error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

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
                    text = L10n.startShuttle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = if (canStartTrip) NeonTheme.Primary else NeonTheme.OnSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
