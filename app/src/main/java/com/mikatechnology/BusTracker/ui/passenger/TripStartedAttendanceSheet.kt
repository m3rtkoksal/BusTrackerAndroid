package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private val ErrorRed = Color(0xFFFF4444)

@Composable
fun TripStartedAttendanceSheet(
    driverName: String,
    isLoading: Boolean,
    pendingStatus: AttendanceStatus?,
    onSelectComing: () -> Unit,
    onSelectNotComing: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .background(NeonTheme.SurfaceContainer)
            .padding(horizontal = 28.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 48.dp, height = 4.dp)
                .clip(CircleShape)
                .background(NeonTheme.SurfaceContainerHigh)
        )

        Icon(
            imageVector = Icons.Default.DirectionsBus,
            contentDescription = null,
            tint = NeonTheme.Secondary,
            modifier = Modifier.size(40.dp)
        )

        Text(
            text = "Servis başladı",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NeonTheme.OnSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = "$driverName servisi yola çıktı. Bugün gelecek misiniz?",
            fontSize = 14.sp,
            color = NeonTheme.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetAttendanceButton(
                title = "GELİYORUM",
                icon = Icons.Default.CheckCircle,
                accent = NeonTheme.Secondary,
                isSelected = pendingStatus == AttendanceStatus.Coming,
                isLoading = isLoading && pendingStatus != AttendanceStatus.Coming,
                onClick = onSelectComing,
                modifier = Modifier.weight(1f)
            )
            SheetAttendanceButton(
                title = "GELMİYORUM",
                icon = Icons.Default.Close,
                accent = ErrorRed,
                isSelected = pendingStatus == AttendanceStatus.NotComing,
                isLoading = isLoading && pendingStatus != AttendanceStatus.NotComing,
                onClick = onSelectNotComing,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Seçiminiz sürücüye anında iletilir.",
            fontSize = 12.sp,
            color = NeonTheme.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Sonra",
            fontSize = 13.sp,
            color = NeonTheme.OnSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(onClick = onDismiss)
        )
    }
}

@Composable
private fun SheetAttendanceButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) accent.copy(alpha = 0.12f) else NeonTheme.SurfaceContainerHigh.copy(alpha = 0.85f)
    val borderColor = if (isSelected) accent.copy(alpha = 0.55f) else NeonTheme.Outline.copy(alpha = 0.25f)
    val contentColor = if (isSelected) accent else NeonTheme.OnSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(26.dp)
                    .shadow(if (isSelected) 8.dp else 0.dp, spotColor = if (isSelected) accent.copy(alpha = 0.65f) else Color.Transparent)
            )
        }
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = contentColor,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
