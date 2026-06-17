package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
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

@Composable
fun SubscriptionExpiringBanner(
    daysRemaining: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NeonTheme.Primary)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = NeonTheme.Background,
            modifier = Modifier.size(16.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = L10n.subscriptionExpiringSoonBanner,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.Background
            )
            Text(
                text = L10n.subscriptionExpiringSoonSubtitle(daysRemaining),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.Background.copy(alpha = 0.8f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = NeonTheme.Background.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
    }
}
