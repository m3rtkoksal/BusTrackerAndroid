package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private val HolidayGold = Color(0xFFFFE04A)

@Composable
fun SparseModeSuggestionSheet(
    comingDays: Int,
    onConfirm: () -> Unit,
    onLater: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 48.dp, height = 4.dp)
                .clip(CircleShape)
                .background(NeonTheme.SurfaceContainerHigh)
        )

        Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = null,
            tint = HolidayGold,
            modifier = Modifier.size(40.dp)
        )

        Text(
            text = L10n.sparseModeSheetTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NeonTheme.OnSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = L10n.sparseModeSheetMessage(comingDays),
            fontSize = 15.sp,
            color = NeonTheme.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        SparseSheetActionButton(
            text = L10n.sparseModeSheetOk.uppercase(),
            accent = NeonTheme.Secondary,
            onClick = onConfirm,
            modifier = Modifier.padding(top = 4.dp)
        )

        SparseSheetActionButton(
            text = L10n.later.uppercase(),
            accent = NeonTheme.OnSurfaceVariant,
            filled = false,
            onClick = onLater
        )
    }
}

@Composable
private fun SparseSheetActionButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    val bg = if (filled) accent.copy(alpha = 0.18f) else Color.Transparent
    val borderAlpha = if (filled) 0.85f else 0.35f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(2.dp, accent.copy(alpha = borderAlpha), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = accent,
            textAlign = TextAlign.Center
        )
    }
}
