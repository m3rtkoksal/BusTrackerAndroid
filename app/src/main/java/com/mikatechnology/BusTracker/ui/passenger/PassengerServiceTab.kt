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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
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
import androidx.compose.material.icons.filled.Lock
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.ShuttleCapabilities
import com.mikatechnology.BusTracker.data.model.ShuttleMember
import com.mikatechnology.BusTracker.data.model.UpcomingService
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.services.PassengerWeatherCardModel
import com.mikatechnology.BusTracker.services.PassengerWeatherService
import com.mikatechnology.BusTracker.ui.driver.SubscriptionExpiringBanner
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private val WarningColor = Color(0xFFFFE04A)
private val ErrorRed = Color(0xFFFF4444)
private val ServiceCardShape = RectangleShape

/** Her servis için attendance durumu */
data class ServiceAttendanceState(
    val service: UpcomingService,
    val rawAttendance: AttendanceStatus,
    val effectiveAttendance: AttendanceStatus
)

@Composable
fun PassengerServiceTab(
    profile: UserProfile,
    nextTwoServices: List<ServiceAttendanceState>,
    nearestUpcomingService: UpcomingService,
    notComingPassengers: List<ShuttleMember>,
    savedMorningPickup: MorningPickup?,
    draftLatitude: Double?,
    draftLongitude: Double?,
    isUpdatingAttendance: Boolean,
    isHolidayModeActive: Boolean,
    holidayModeSubtitle: String,
    holidayModeDetailLine: String,
    capabilities: ShuttleCapabilities,
    showComingBlockedWithoutPickupHint: Boolean = false,
    onAttendanceSelected: (UpcomingService, AttendanceStatus) -> Unit,
    onOpenHolidayModePicker: () -> Unit,
    onShowSubscriptionGate: () -> Unit,
    onGoToSubscription: () -> Unit,
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
    val delayNoticeRevision by ShuttleStore.shared.delayNoticeRevision.collectAsStateWithLifecycle()
    val serviceDelayMinutes = remember(nearestUpcomingService, delayNoticeRevision) {
        ShuttleStore.shared.delayNoticeMinutesFor(nearestUpcomingService.dateKey)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (capabilities.isExpiringSoon && capabilities.daysRemaining != null) {
            SubscriptionExpiringBanner(
                daysRemaining = capabilities.daysRemaining!!,
                onTap = onGoToSubscription
            )
        }

        if (serviceDelayMinutes != null) {
            ServiceDelayNoticeSection(minutes = serviceDelayMinutes!!)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            nextTwoServices.forEach { state ->
                ServiceAttendanceCard(
                    state = state,
                    isHolidayModeActive = isHolidayModeActive,
                    isUpdatingAttendance = isUpdatingAttendance,
                    isLocked = !capabilities.canSetAttendance,
                    onAttendanceSelected = { status ->
                        if (!capabilities.canSetAttendance) {
                            onShowSubscriptionGate()
                        } else {
                            onAttendanceSelected(state.service, status)
                        }
                    }
                )
            }

            Text(
                text = if (isHolidayModeActive) L10n.attendanceHolidayHint else L10n.attendanceHint,
                fontSize = 10.sp,
                color = NeonTheme.Outline,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (showComingBlockedWithoutPickupHint) {
                Text(
                    text = L10n.comingBlockedWithoutPickup,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (notComingPassengers.isNotEmpty()) {
            NotComingPassengersSection(
                service = nearestUpcomingService,
                passengers = notComingPassengers
            )
        }

        HolidayModeServiceCard(
            isActive = isHolidayModeActive,
            subtitle = holidayModeSubtitle,
            detailLine = holidayModeDetailLine,
            isLocked = !capabilities.canSetHolidayMode,
            onClick = {
                if (!capabilities.canSetHolidayMode) {
                    onShowSubscriptionGate()
                } else {
                    onOpenHolidayModePicker()
                }
            }
        )

        PassengerClothingAdviceCard(
            model = pickupWeather,
            isLoading = weatherLatitude != null && weatherLongitude != null && pickupWeatherLoading,
            emptyMessage = if (weatherLatitude == null || weatherLongitude == null) L10n.weatherNeedsPickup else null
        )
    }
}

@Composable
private fun ServiceDelayNoticeSection(minutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ServiceCardShape)
            .background(WarningColor.copy(alpha = 0.1f))
            .border(1.5.dp, WarningColor.copy(alpha = 0.45f), ServiceCardShape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = WarningColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = L10n.passengerServiceDelayed(minutes),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeonTheme.OnSurface
        )
    }
}

@Composable
private fun NotComingPassengersSection(
    service: UpcomingService,
    passengers: List<ShuttleMember>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ServiceCardShape)
            .background(NeonTheme.SurfaceContainer)
            .border(
                width = 1.5.dp,
                color = NeonTheme.OnSurfaceVariant.copy(alpha = 0.55f),
                shape = ServiceCardShape
            )
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = service.session.icon,
                fontSize = 16.sp
            )
            Text(
                text = L10n.serviceNotComingListTitle(service.relativeDisplayName()).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = NeonTheme.OnSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = NeonTheme.OnSurfaceVariant.copy(alpha = 0.7f),
                    shape = ServiceCardShape
                )
        ) {
            passengers.forEachIndexed { index, member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonTheme.SurfaceContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = member.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonTheme.OnSurface,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                if (index < passengers.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(NeonTheme.OnSurfaceVariant.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceAttendanceCard(
    state: ServiceAttendanceState,
    isHolidayModeActive: Boolean,
    isUpdatingAttendance: Boolean,
    isLocked: Boolean = false,
    onAttendanceSelected: (AttendanceStatus) -> Unit
) {
    val showChoice = state.rawAttendance != AttendanceStatus.Unknown ||
        (isHolidayModeActive && state.effectiveAttendance != AttendanceStatus.Unknown)
    val choiceStatus = if (state.rawAttendance != AttendanceStatus.Unknown) {
        state.rawAttendance
    } else {
        state.effectiveAttendance
    }

    val comingSelected = state.rawAttendance == AttendanceStatus.Coming
    val notComingSelected = state.rawAttendance == AttendanceStatus.NotComing ||
        (isHolidayModeActive &&
            state.rawAttendance == AttendanceStatus.Unknown &&
            state.effectiveAttendance == AttendanceStatus.NotComing)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ServiceCardShape)
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = NeonTheme.Outline.copy(alpha = 0.15f),
                shape = ServiceCardShape
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.service.session.icon,
                    fontSize = 16.sp
                )
                Column {
                    Text(
                        text = state.service.relativeDisplayName().uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NeonTheme.OnSurface
                    )
                    Text(
                        text = state.service.displayDate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonTheme.OnSurfaceVariant
                    )
                }
            }

            if (showChoice) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (choiceStatus == AttendanceStatus.Coming) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (choiceStatus == AttendanceStatus.Coming) NeonTheme.Secondary else ErrorRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = choiceStatus.selfChoiceLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (choiceStatus == AttendanceStatus.Coming) NeonTheme.Secondary else ErrorRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttendanceButton(
                title = L10n.attendanceComingSelf.uppercase(),
                icon = Icons.Default.CheckCircle,
                accent = NeonTheme.Secondary,
                isSelected = comingSelected,
                isLoading = isUpdatingAttendance && !comingSelected,
                enabled = true,
                isLocked = isLocked,
                onClick = { onAttendanceSelected(AttendanceStatus.Coming) },
                modifier = Modifier.weight(1f)
            )

            AttendanceButton(
                title = L10n.attendanceNotComingSelf.uppercase(),
                icon = Icons.Default.Close,
                accent = ErrorRed,
                isSelected = notComingSelected,
                isLoading = isUpdatingAttendance && !notComingSelected,
                enabled = true,
                isLocked = isLocked,
                onClick = { onAttendanceSelected(AttendanceStatus.NotComing) },
                modifier = Modifier.weight(1f)
            )
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
    isLocked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) accent.copy(alpha = 0.12f) else NeonTheme.SurfaceContainerHigh.copy(alpha = 0.85f)
    val borderColor = if (isSelected) accent.copy(alpha = 0.55f) else NeonTheme.Outline.copy(alpha = 0.25f)
    val contentColor = if (isSelected) accent else NeonTheme.OnSurfaceVariant
    val alpha = if (isLocked) 0.5f else 1f

    Column(
        modifier = modifier
            .clip(ServiceCardShape)
            .background(bg.copy(alpha = bg.alpha * alpha))
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
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = contentColor.alpha * alpha),
                    modifier = Modifier
                        .size(26.dp)
                        .shadow(if (isSelected) 8.dp else 0.dp, spotColor = if (isSelected) accent.copy(alpha = 0.65f) else Color.Transparent)
                )
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeonTheme.OnSurfaceVariant,
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = contentColor.copy(alpha = contentColor.alpha * alpha),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
