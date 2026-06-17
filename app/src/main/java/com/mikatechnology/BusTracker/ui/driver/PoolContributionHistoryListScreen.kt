package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.PoolContributionHistoryItem
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun PoolContributionHistoryListScreen(
    items: List<PoolContributionHistoryItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonTheme.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeonTheme.Surface.copy(alpha = 0.8f))
                .padding(horizontal = 16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryBackButton(onClick = onBack)
            Spacer(modifier = Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NeonTheme.Primary.copy(alpha = 0.2f))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = L10n.poolContributionHistoryTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = NeonTheme.OnSurface
                )
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(NeonTheme.Primary)
                )
            }

            PoolContributionHistoryCard(items = items)
        }
    }
}

@Composable
fun PoolContributionHistoryCard(
    items: List<PoolContributionHistoryItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(NeonTheme.SurfaceContainerHigh)
            .border(1.dp, NeonTheme.Primary.copy(alpha = 0.35f))
    ) {
        if (items.isEmpty()) {
            Text(
                text = L10n.poolContributionHistoryEmpty,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.OnSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            items.forEachIndexed { index, item ->
                PoolContributionHistoryRow(item)
                if (index < items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(NeonTheme.Outline.copy(alpha = 0.2f))
                    )
                }
            }
        }
    }
}

@Composable
fun PoolContributionHistoryRow(item: PoolContributionHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.memberName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeonTheme.OnSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = item.amountText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = NeonTheme.Primary
        )
    }
}

@Composable
private fun HistoryBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(SettingsCardShape)
            .background(NeonTheme.SurfaceContainer)
            .border(1.dp, NeonTheme.Secondary.copy(alpha = 0.35f), SettingsCardShape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = L10n.back,
            tint = NeonTheme.Secondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
