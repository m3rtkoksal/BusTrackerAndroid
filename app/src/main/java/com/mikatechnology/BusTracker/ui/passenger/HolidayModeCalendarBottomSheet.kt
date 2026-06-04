package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.HolidayMode
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import java.util.Date

private val HolidayAccent = Color(0xFFFFE04A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayModeCalendarBottomSheet(
    isHolidayActive: Boolean,
    activeEndDateKey: String?,
    isSaving: Boolean,
    onConfirm: (Date) -> Unit,
    onEndHoliday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayMillis = remember { HolidayMode.startOfTodayMillis() }
    val initialMillis = remember(activeEndDateKey) {
        activeEndDateKey
            ?.let { HolidayMode.dateFromKey(it)?.time }
            ?.let { maxOf(it, todayMillis) }
            ?: todayMillis
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= todayMillis
        }
    )

    val sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(NeonTheme.SurfaceContainerLow)
            .holidaySheetTopBorder(cornerRadius = 40.dp)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 20.dp)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(NeonTheme.SurfaceContainerHighest)
            )

            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = HolidayAccent,
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = L10n.holidayModeTitle,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = L10n.holidayModeCalendarHint,
                fontSize = 14.sp,
                color = NeonTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 16.dp)
            )

            DatePicker(
                state = datePickerState,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HolidayAccent.copy(alpha = 0.14f))
                    .border(1.dp, HolidayAccent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !isSaving) {
                        val millis = datePickerState.selectedDateMillis ?: todayMillis
                        onConfirm(Date(millis))
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = HolidayAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = L10n.holidayModeSave.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = HolidayAccent
                    )
                }
            }

            if (isHolidayActive) {
                Text(
                    text = L10n.holidayModeEndEarly,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTheme.OnSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSaving, onClick = onEndHoliday)
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HolidayModePickerOverlay(
    isHolidayActive: Boolean,
    activeEndDateKey: String?,
    isSaving: Boolean,
    onConfirm: (Date) -> Unit,
    onEndHoliday: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
        )
        HolidayModeCalendarBottomSheet(
            isHolidayActive = isHolidayActive,
            activeEndDateKey = activeEndDateKey,
            isSaving = isSaving,
            onConfirm = onConfirm,
            onEndHoliday = onEndHoliday,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun Modifier.holidaySheetTopBorder(
    strokeWidth: Dp = 1.dp,
    cornerRadius: Dp = 40.dp
): Modifier = drawBehind {
    val stroke = strokeWidth.toPx()
    val radius = cornerRadius.toPx()
    val half = stroke / 2f
    val arcSize = Size(radius * 2f, radius * 2f)
    val strokeStyle = Stroke(width = stroke)
    val topColor = HolidayAccent.copy(alpha = 0.45f)
    val sideColor = HolidayAccent.copy(alpha = 0.12f)

    drawLine(
        color = topColor,
        start = Offset(radius, half),
        end = Offset(size.width - radius, half),
        strokeWidth = stroke
    )
    drawArc(
        color = topColor,
        topLeft = Offset(half, half),
        size = arcSize,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        style = strokeStyle
    )
    drawArc(
        color = topColor,
        topLeft = Offset(size.width - arcSize.width - half, half),
        size = arcSize,
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        style = strokeStyle
    )
    drawLine(
        color = sideColor,
        start = Offset(half, radius),
        end = Offset(half, size.height),
        strokeWidth = stroke
    )
    drawLine(
        color = sideColor,
        start = Offset(size.width - half, radius),
        end = Offset(size.width - half, size.height),
        strokeWidth = stroke
    )
}
