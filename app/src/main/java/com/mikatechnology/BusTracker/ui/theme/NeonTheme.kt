package com.mikatechnology.BusTracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object NeonTheme {
    val Background = Color(0xFF0A0A12)
    val Surface = Color(0xFF0F0F1A)
    val SurfaceContainer = Color(0xFF141422)
    val SurfaceContainerLow = Color(0xFF111118)
    val SurfaceContainerHigh = Color(0xFF1E1E30)
    val SurfaceContainerHighest = Color(0xFF28283E)
    val SurfaceBright = Color(0xFF1A1A2E)
    val OnSurface = Color(0xFFE8E0F0)
    val OnSurfaceVariant = Color(0xFFA098B0)
    val Primary = Color(0xFFFF2D78)
    val Secondary = Color(0xFF00FFCC)
    /** Harita: “Biniş noktamı kaydet” — iOS `mapSaveAction` ile aynı */
    val MapSaveAction = Color(0xFF6EFFF7)
    val Outline = Color(0xFF5A5068)
    val Error = Color(0xFFFF4444)
    /** Harita: sürücü canlı konum pini */
    val MapDriverPin = Color(0xFF4DA3FF)
    /** Harita: yolcu biniş pini */
    val MapPickupPin = Color(0xFFFF4444)
}

@Composable
fun NeonBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeonTheme.Background)
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-120).dp, y = (-280).dp)
                .clip(CircleShape)
                .background(NeonTheme.Primary.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 140.dp, y = 320.dp)
                .clip(CircleShape)
                .background(NeonTheme.Secondary.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NeonTheme.Primary.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
