package com.mikatechnology.BusTracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

/** Ayarlar sekmesi kartları — köşe radius 0. */
val SettingsCardShape = RectangleShape

@Composable
fun SettingsSignOutRow(
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
                color = NeonTheme.Error.copy(alpha = 0.3f),
                shape = SettingsCardShape
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = NeonTheme.Error
        )
        Text(
            text = L10n.signOut,
            color = NeonTheme.Error,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun SettingsDeleteAccountLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NeonTheme.Outline.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = L10n.deleteAccount,
            color = NeonTheme.Error.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
