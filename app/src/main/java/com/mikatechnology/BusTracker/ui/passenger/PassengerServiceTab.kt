package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



private val WarningColor = Color(0xFFFFE04A)
private val ErrorRed = Color(0xFFFF4444)

@Composable
fun PassengerServiceTab(
    profile: UserProfile,
    isTripActive: Boolean,
    myAttendance: AttendanceStatus,
    savedMorningPickup: MorningPickup?,
    isUpdatingAttendance: Boolean,
    onAttendanceSelected: (AttendanceStatus) -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column {
            Text(
                text = profile.groupName.uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isTripActive) NeonTheme.Secondary else NeonTheme.Outline)
                        .shadow(
                            elevation = if (isTripActive) 4.dp else 0.dp,
                            spotColor = if (isTripActive) NeonTheme.Secondary.copy(alpha = 0.8f) else Color.Transparent
                        )
                )
                Text(
                    text = if (isTripActive) "Sürücü konumu aktif" else "Servis henüz başlamadı",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTripActive) NeonTheme.Secondary else NeonTheme.OnSurfaceVariant
                )
            }
        }

        // Attendance Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonTheme.SurfaceContainer)
                .border(
                    width = 1.dp,
                    color = NeonTheme.Secondary.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "BUGÜN GELECEK MİSİNİZ?",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = NeonTheme.OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (myAttendance != AttendanceStatus.Unknown) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (myAttendance == AttendanceStatus.Coming) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (myAttendance == AttendanceStatus.Coming) NeonTheme.Secondary else ErrorRed
                    )
                    Text(
                        text = "Seçiminiz: ${myAttendance.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonTheme.OnSurface,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceButton(
                    title = "GELİYORUM",
                    icon = Icons.Default.CheckCircle,
                    accent = NeonTheme.Secondary,
                    isSelected = myAttendance == AttendanceStatus.Coming,
                    isLoading = isUpdatingAttendance && myAttendance != AttendanceStatus.Coming,
                    enabled = true,
                    onClick = { onAttendanceSelected(AttendanceStatus.Coming) },
                    modifier = Modifier.weight(1f)
                )

                AttendanceButton(
                    title = "GELMİYORUM",
                    icon = Icons.Default.Close,
                    accent = ErrorRed,
                    isSelected = myAttendance == AttendanceStatus.NotComing,
                    isLoading = isUpdatingAttendance && myAttendance != AttendanceStatus.NotComing,
                    enabled = true,
                    onClick = { onAttendanceSelected(AttendanceStatus.NotComing) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Seçiminiz sürücüye kaydedilir. Servis bitince yeniden seçmeniz gerekir.",
                fontSize = 10.sp,
                color = NeonTheme.Outline,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Pickup Summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonTheme.SurfaceContainer)
                .border(
                    width = 1.dp,
                    color = NeonTheme.Outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "BİNİŞ NOKTASI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = NeonTheme.OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (savedMorningPickup != null) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(savedMorningPickup.updatedAt)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonTheme.Secondary
                    )
                    Text(
                        text = "Kayıtlı: $time",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonTheme.Secondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            } else {
                Text(
                    text = "Henüz biniş noktası kaydetmediniz.",
                    color = NeonTheme.OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonTheme.SurfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = NeonTheme.Secondary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onOpenMap() }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = NeonTheme.Secondary
                )
                Text(
                    text = if (savedMorningPickup == null) "HARİTADA BELİRLE" else "HARİTADA DÜZENLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = NeonTheme.Secondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AttendanceButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    isSelected: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
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
            .border( if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp) )
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(26.dp)
            )
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
