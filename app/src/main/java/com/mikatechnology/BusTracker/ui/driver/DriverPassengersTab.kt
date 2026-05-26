package com.mikatechnology.BusTracker.ui.driver

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.ShuttleMember
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.services.LocationAuthStatus
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private val DangerColor = NeonTheme.Error
private val WarningColor = androidx.compose.ui.graphics.Color(0xFFFFE04A)

@Composable
fun DriverPassengersTab(
    profile: UserProfile,
    passengers: List<ShuttleMember>,
    stats: DriverPassengerStats,
    isTripActive: Boolean,
    isTripBusy: Boolean,
    locationAuthStatus: LocationAuthStatus,
    onToggleTrip: () -> Unit,
    onCopyCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonTheme.Secondary)
                )
                Text(
                    text = "SERVİS ADI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = NeonTheme.OnSurfaceVariant
                )
            }
            Text(
                text = profile.groupName.uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface,
                letterSpacing = (-0.5).sp
            )
        }

        DriverServiceCodeCard(
            code = profile.groupCode,
            onCopy = onCopyCode,
            onShare = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "ServisTakip servis kodum: ${profile.groupCode}")
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            }
        )

        TripControlSection(
            isTripActive = isTripActive,
            isBusy = isTripBusy,
            onToggleTrip = onToggleTrip
        )

        StatsGrid(stats = stats)

        if (passengers.isEmpty()) {
            DriverEmptyPassengersState()
        } else {
            PassengerListSection(passengers = passengers)
        }

        if (locationAuthStatus.isDenied) {
            Text(
                text = "Konum izni kapalı. Ayarlar'dan izin verin.",
                fontSize = 12.sp,
                color = DangerColor
            )
        } else if (isTripActive && locationAuthStatus.needsAlwaysAuthorization) {
            Text(
                text = "Arka planda konum paylaşımı için Ayarlar'dan \"Her Zaman\" iznini seçin.",
                fontSize = 12.sp,
                color = WarningColor
            )
        }
    }
}

@Composable
private fun DriverServiceCodeCard(
    code: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.7f))
            .border(1.dp, NeonTheme.Primary.copy(alpha = 0.2f))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SERVİS KODU",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = NeonTheme.Primary,
            modifier = Modifier.shadow(6.dp, spotColor = NeonTheme.Primary.copy(alpha = 0.6f))
        )
        Text(
            text = code.uppercase(),
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 6.sp,
            color = NeonTheme.OnSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CodeActionButton(
                title = "Paylaş",
                icon = Icons.Default.Share,
                accent = NeonTheme.Primary,
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = onShare
            )
            CodeActionButton(
                title = "Kopyala",
                icon = Icons.Default.ContentCopy,
                accent = NeonTheme.OnSurfaceVariant,
                filled = false,
                modifier = Modifier.weight(1f),
                onClick = onCopy
            )
        }
    }
}

@Composable
private fun CodeActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (filled) NeonTheme.Primary.copy(alpha = 0.1f) else NeonTheme.SurfaceContainer
            )
            .border(1.dp, accent.copy(alpha = if (filled) 0.4f else 0.3f))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = accent,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun TripControlSection(
    isTripActive: Boolean,
    isBusy: Boolean,
    onToggleTrip: () -> Unit
) {
    val accent = if (isTripActive) DangerColor else NeonTheme.Primary
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isBusy, onClick = onToggleTrip)
                .background(NeonTheme.Background)
                .border(2.dp, accent)
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = accent,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isTripActive) Icons.Default.Stop else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isTripActive) "SERVİSİ DURDUR" else "SERVİSİ BAŞLAT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = accent,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
        Text(
            text = if (isTripActive) "Konum paylaşılıyor" else "Başlatma hazır",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            color = NeonTheme.Outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatsGrid(stats: DriverPassengerStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            title = "GELECEK",
            value = stats.coming.toString(),
            valueColor = NeonTheme.Secondary,
            borderColor = NeonTheme.Secondary.copy(alpha = 0.3f),
            trailingIcon = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "GELMEYECEK",
            value = stats.notComing.toString(),
            valueColor = DangerColor,
            borderColor = DangerColor.copy(alpha = 0.3f),
            trailingIcon = Icons.Default.Cancel,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "BELİRTMEDİ",
            value = stats.unknown.toString(),
            valueColor = WarningColor,
            borderColor = WarningColor.copy(alpha = 0.35f),
            trailingIcon = Icons.Default.Help,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(NeonTheme.SurfaceContainer)
            .border(1.dp, borderColor)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = NeonTheme.OnSurfaceVariant,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = if (valueColor == NeonTheme.Secondary) {
                    Modifier.shadow(6.dp, spotColor = NeonTheme.Secondary.copy(alpha = 0.5f))
                } else {
                    Modifier
                }
            )
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = valueColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun DriverEmptyPassengersState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(1.dp, NeonTheme.Primary.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = NeonTheme.Primary.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "YOLCU BEKLENİYOR",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = NeonTheme.OnSurfaceVariant
            )
            Text(
                text = "Yolcular yukarıdaki servis kodunu kullanarak katıldığında burada görünecek.",
                fontSize = 12.sp,
                color = NeonTheme.Outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        }
    }
}

@Composable
private fun PassengerListSection(passengers: List<ShuttleMember>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "YOLCU LİSTESİ",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            color = NeonTheme.OnSurfaceVariant
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonTheme.Outline.copy(alpha = 0.3f))
        ) {
            passengers.forEachIndexed { index, member ->
                PassengerRow(member = member)
                if (index < passengers.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NeonTheme.Outline.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}

@Composable
private fun PassengerRow(member: ShuttleMember) {
    val color = attendanceColor(member.attendance)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = member.attendance.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = member.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.OnSurface
            )
            Text(
                text = member.attendance.title,
                fontSize = 12.sp,
                color = NeonTheme.OnSurfaceVariant
            )
        }
    }
}

private fun attendanceColor(status: AttendanceStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        AttendanceStatus.Coming -> NeonTheme.Secondary
        AttendanceStatus.NotComing -> DangerColor
        AttendanceStatus.Unknown -> WarningColor
    }
}
