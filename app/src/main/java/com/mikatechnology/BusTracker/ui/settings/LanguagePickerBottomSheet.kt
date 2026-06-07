package com.mikatechnology.BusTracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.localization.AppLanguage
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun LanguagePickerBottomSheet(
    selectedLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(NeonTheme.SurfaceContainerLow)
            .sheetTopBorder(
                topColor = NeonTheme.Secondary.copy(alpha = 0.45f),
                sideColor = NeonTheme.Secondary.copy(alpha = 0.12f),
                cornerRadius = 40.dp
            )
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
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = NeonTheme.Secondary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = L10n.settingsLanguage,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppLanguage.entries.forEach { language ->
                    LanguageOptionRow(
                        language = language,
                        isSelected = language == selectedLanguage,
                        onClick = { onSelect(language) }
                    )
                }
            }
        }
    }
}

private fun Modifier.sheetTopBorder(
    topColor: Color,
    sideColor: Color,
    strokeWidth: Dp = 1.dp,
    cornerRadius: Dp = 40.dp
): Modifier = drawBehind {
    val stroke = strokeWidth.toPx()
    val radius = cornerRadius.toPx()
    val half = stroke / 2f
    val arcSize = Size(radius * 2f, radius * 2f)
    val strokeStyle = Stroke(width = stroke)

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

@Composable
private fun LanguageOptionRow(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) NeonTheme.Secondary.copy(alpha = 0.14f)
                else NeonTheme.SurfaceContainerHigh.copy(alpha = 0.85f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NeonTheme.Secondary.copy(alpha = 0.55f)
                else NeonTheme.Outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) NeonTheme.Secondary else NeonTheme.OnSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonTheme.Secondary
            )
        }
    }
}

@Composable
fun LanguageSettingsRow(
    currentLanguage: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(NeonTheme.SurfaceContainer)
            .border(
                width = 1.dp,
                color = NeonTheme.Outline.copy(alpha = 0.3f),
                shape = SettingsCardShape
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = L10n.settingsLanguage.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = NeonTheme.OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = currentLanguage.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.OnSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeonTheme.OnSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguagePickerOverlay(
    selectedLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
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
        LanguagePickerBottomSheet(
            selectedLanguage = selectedLanguage,
            onSelect = onSelect,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
