package com.mikatechnology.BusTracker.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

/**
 * Hafif vignette — harita karolarını kapatmamalı.
 */
@Composable
fun NeonMapOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        NeonScanlineOverlay()
    }
}

@Composable
private fun NeonScanlineOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = NeonTheme.Secondary.copy(alpha = 0.015f),
                topLeft = Offset(0f, y),
                size = Size(size.width, 2f)
            )
            y += 4f
        }
    }
}
