package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.settings.SettingsDeleteAccountFooter
import com.mikatechnology.BusTracker.ui.settings.SettingsSignOutRow
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun PassengerSettingsTab(
    profile: UserProfile,
    onCopyCode: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsRow(
                title = "SERVİS KODU",
                value = profile.groupCode,
                onClick = onCopyCode,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = NeonTheme.Secondary
                    )
                }
            )

            SettingsRow(
                title = "ADINIZ",
                value = profile.name,
                onClick = null
            )

            SettingsRow(
                title = "SERVİS",
                value = profile.groupName,
                onClick = null
            )

            SettingsSignOutRow(onClick = onSignOut)
        }

        SettingsDeleteAccountFooter(onClick = onDeleteAccount)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    onClick: (() -> Unit)?,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(NeonTheme.SurfaceContainer)
            .border(
                width = 1.dp,
                color = NeonTheme.Outline.copy(alpha = 0.3f),
                shape = SettingsCardShape
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = NeonTheme.OnSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        trailingIcon?.invoke()
    }
}
