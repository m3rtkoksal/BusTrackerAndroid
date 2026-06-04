package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.services.PassengerWeatherCardModel
import com.mikatechnology.BusTracker.services.PassengerWeatherService
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import java.text.SimpleDateFormat
import java.util.Locale

private val WarningColor = Color(0xFFFFE04A)
private val ErrorRed = Color(0xFFFF4444)
private val ServiceCardShape = RectangleShape

@Composable
fun PassengerServiceTab(
    profile: UserProfile,
    isTripActive: Boolean,
    myAttendance: AttendanceStatus,
    savedMorningPickup: MorningPickup?,
    draftLatitude: Double?,
    draftLongitude: Double?,
    isUpdatingAttendance: Boolean,
    isHolidayModeActive: Boolean,
    holidayModeSubtitle: String,
    holidayModeDetailLine: String,
    onAttendanceSelected: (AttendanceStatus) -> Unit,
    onOpenHolidayModePicker: () -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pickupWeather by remember { mutableStateOf<PassengerWeatherCardModel?>(null) }
    var pickupWeatherLoading by remember { mutableStateOf(false) }

    val weatherLatitude = savedMorningPickup?.latitude ?: draftLatitude
    val weatherLongitude = savedMorningPickup?.longitude ?: draftLongitude
    val weatherCoordinateKey = weatherLatitude?.let { lat ->
        weatherLongitude?.let { lng -> "$lat,$lng" }
    } ?: "none"

    LaunchedEffect(weatherCoordinateKey) {
        val lat = weatherLatitude
        val lng = weatherLongitude
        if (lat == null || lng == null) {
            pickupWeather = null
            pickupWeatherLoading = false
            return@LaunchedEffect
        }
        val cached = PassengerWeatherService.cachedModel(context, lat, lng)
        if (cached != null) {
            pickupWeather = cached
            pickupWeatherLoading = false
            return@LaunchedEffect
        }
        pickupWeatherLoading = true
        pickupWeather = PassengerWeatherService.load(context, lat, lng)
        pickupWeatherLoading = false
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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
                    text = if (isTripActive) L10n.waitingForDriverLocation else L10n.shuttleNotStarted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTripActive) NeonTheme.Secondary else NeonTheme.OnSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ServiceCardShape)
                .background(NeonTheme.SurfaceContainer)
                .border(
                    width = 1.dp,
                    color = NeonTheme.Secondary.copy(alpha = 0.22f),
                    shape = ServiceCardShape
                )
                .padding(16.dp)
        ) {
            Text(
                text = L10n.attendanceTodayQuestion,
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
                        text = L10n.yourChoice(myAttendance.title),
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
                    title = L10n.attendanceComingSelf.uppercase(),
                    icon = Icons.Default.CheckCircle,
                    accent = NeonTheme.Secondary,
                    isSelected = myAttendance == AttendanceStatus.Coming,
                    isLoading = isUpdatingAttendance && myAttendance != AttendanceStatus.Coming,
                    enabled = true,
                    onClick = { onAttendanceSelected(AttendanceStatus.Coming) },
                    modifier = Modifier.weight(1f)
                )

                AttendanceButton(
                    title = L10n.attendanceNotComingSelf.uppercase(),
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
                text = L10n.attendanceHint,
                fontSize = 10.sp,
                color = NeonTheme.Outline,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        HolidayModeServiceCard(
            isActive = isHolidayModeActive,
            subtitle = holidayModeSubtitle,
            detailLine = holidayModeDetailLine,
            onClick = onOpenHolidayModePicker
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ServiceCardShape)
                .background(NeonTheme.SurfaceContainer)
                .border(
                    width = 1.dp,
                    color = NeonTheme.Outline.copy(alpha = 0.25f),
                    shape = ServiceCardShape
                )
                .padding(16.dp)
        ) {
            Text(
                text = L10n.pickupPoint,
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
                        text = L10n.savedAt(time),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonTheme.Secondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            } else {
                Text(
                    text = L10n.noPickupSaved,
                    color = NeonTheme.OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ServiceCardShape)
                    .background(NeonTheme.SurfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = NeonTheme.Secondary.copy(alpha = 0.45f),
                        shape = ServiceCardShape
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
                    text = if (savedMorningPickup == null) L10n.setOnMap else L10n.editOnMap,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = NeonTheme.Secondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        PassengerClothingAdviceCard(
            model = pickupWeather,
            isLoading = weatherLatitude != null && weatherLongitude != null && pickupWeatherLoading,
            emptyMessage = if (weatherLatitude == null || weatherLongitude == null) L10n.weatherNeedsPickup else null
        )
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
            .clip(ServiceCardShape)
            .background(bg)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, ServiceCardShape)
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
