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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private val HolidayAccent = Color(0xFFFFE04A)

@Composable
fun HolidayModeServiceCard(
    isActive: Boolean,
    subtitle: String,
    detailLine: String,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RectangleShape
    val gradientAlpha = if (isActive) 0.12f else 0.06f
    val alpha = if (isLocked) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isActive && !isLocked) 10.dp else 4.dp,
                spotColor = HolidayAccent.copy(alpha = if (isActive) 0.18f else 0.08f)
            )
            .clip(shape)
            .background(NeonTheme.SurfaceContainer)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HolidayAccent.copy(alpha = gradientAlpha * alpha),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
            }
            .holidayCardBorder(isActive = isActive)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HolidayAccent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = HolidayAccent,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.holidayModeTitle.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    color = HolidayAccent
                )
                if (isActive) {
                    Text(
                        text = L10n.holidayModeBadgeActive,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = NeonTheme.Background,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HolidayAccent)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = subtitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = detailLine,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.OnSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (isLocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NeonTheme.OnSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp).size(16.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = HolidayAccent.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun Modifier.holidayCardBorder(isActive: Boolean): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val accent = HolidayAccent
    val topAlpha = if (isActive) 0.55f else 0.35f
    drawRect(
        color = accent,
        topLeft = Offset.Zero,
        size = Size(stroke, size.height)
    )
    drawLine(
        color = accent.copy(alpha = topAlpha),
        start = Offset(stroke, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = stroke
    )
    drawLine(
        color = accent.copy(alpha = topAlpha),
        start = Offset(stroke, size.height - stroke),
        end = Offset(size.width, size.height - stroke),
        strokeWidth = stroke
    )
    drawLine(
        color = accent.copy(alpha = 0.12f),
        start = Offset(size.width - stroke, 0f),
        end = Offset(size.width - stroke, size.height),
        strokeWidth = stroke
    )
}
