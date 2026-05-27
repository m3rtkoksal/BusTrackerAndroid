package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun PassengerSettingsTab(
    profile: UserProfile,
    onCopyCode: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group Code
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

        // Name
        SettingsRow(
            title = "ADINIZ",
            value = profile.name,
            onClick = null
        )

        // Group Name
        SettingsRow(
            title = "SERVİS",
            value = profile.groupName,
            onClick = null
        )

        Spacer(modifier = Modifier.weight(1f))

        // Logout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NeonTheme.SurfaceContainer)
                .border(
                    width = 1.dp,
                    color = Color(0xFFFF4444).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onSignOut() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = Color(0xFFFF4444)
            )
            Text(
                text = "Çıkış Yap",
                color = Color(0xFFFF4444),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
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
            .clip(RoundedCornerShape(8.dp))
            .background(NeonTheme.SurfaceContainer)
            .border(
                width = 1.dp,
                color = NeonTheme.Outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
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
