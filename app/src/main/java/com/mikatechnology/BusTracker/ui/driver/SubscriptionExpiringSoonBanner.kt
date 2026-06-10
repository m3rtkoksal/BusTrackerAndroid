package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private val WarningColor = androidx.compose.ui.graphics.Color(0xFFFFE04A)

@Composable
fun SubscriptionExpiringSoonBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .background(NeonTheme.SurfaceContainer, SettingsCardShape)
            .border(1.dp, WarningColor.copy(alpha = 0.45f), SettingsCardShape)
            .padding(14.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = WarningColor
    )
}
