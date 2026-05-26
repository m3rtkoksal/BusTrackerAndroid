package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun DriverTabBar(
    selectedTab: DriverHomeTab,
    onTabSelected: (DriverHomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NeonTheme.SurfaceContainerLow)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .shadow(
                    elevation = 8.dp,
                    spotColor = NeonTheme.Secondary.copy(alpha = 0.15f),
                    ambientColor = NeonTheme.Secondary.copy(alpha = 0.15f)
                )
                .background(NeonTheme.Secondary.copy(alpha = 0.2f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DriverHomeTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val accent = if (tab == DriverHomeTab.Map) NeonTheme.Primary else NeonTheme.Secondary
                val foreground = if (isSelected) accent else NeonTheme.Outline
                val glow = if (isSelected) accent.copy(alpha = 0.55f) else Color.Transparent

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = foreground,
                        modifier = Modifier
                            .size(24.dp)
                            .shadow(
                                elevation = if (isSelected) 8.dp else 0.dp,
                                spotColor = glow,
                                ambientColor = glow
                            )
                    )
                    Text(
                        text = tab.title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = (-0.5).sp,
                        color = foreground
                    )
                }
            }
        }
    }
}
