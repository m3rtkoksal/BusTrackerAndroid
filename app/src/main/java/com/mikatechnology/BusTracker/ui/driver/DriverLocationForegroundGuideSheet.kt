package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun DriverLocationForegroundGuideSheet(
    waitingForSettingsReturn: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    bodyGuide: String = L10n.driverLocationForegroundBody,
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 48.dp, height = 4.dp)
                .clip(CircleShape)
                .background(NeonTheme.SurfaceContainerHigh)
        )

        Text(
            text = L10n.driverLocationForegroundTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NeonTheme.OnSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (waitingForSettingsReturn) {
                L10n.locationPermissionBodySettings
            } else {
                bodyGuide
            },
            fontSize = 14.sp,
            color = NeonTheme.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        PermissionGuideStep(number = 1, text = L10n.locationForegroundSettingsStep1)
        PermissionGuideStep(number = 2, text = L10n.locationForegroundSettingsStep2)
        PermissionGuideStep(number = 3, text = L10n.locationForegroundSettingsStep3)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonTheme.Primary.copy(alpha = 0.15f))
                .border(1.dp, NeonTheme.Primary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenSettings)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = L10n.goToSettings,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                color = NeonTheme.Primary
            )
        }

        Text(
            text = L10n.cancel,
            fontSize = 13.sp,
            color = NeonTheme.OnSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(onClick = onDismiss)
        )
    }
}

@Composable
internal fun PermissionGuideStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NeonTheme.Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.Primary
            )
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = NeonTheme.OnSurface,
            modifier = Modifier.weight(1f),
            lineHeight = 21.sp
        )
    }
}
