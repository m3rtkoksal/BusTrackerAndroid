package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun SubscriptionGateSheet(
    onDismiss: () -> Unit,
    onGoToSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = sheetShape,
                spotColor = NeonTheme.Secondary.copy(alpha = 0.12f),
                ambientColor = NeonTheme.Secondary.copy(alpha = 0.12f)
            )
            .clip(sheetShape)
            .background(NeonTheme.SurfaceContainer)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonTheme.Secondary.copy(alpha = 0.45f),
                        NeonTheme.Secondary.copy(alpha = 0.12f)
                    )
                ),
                shape = sheetShape
            )
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 20.dp)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(NeonTheme.SurfaceContainerHigh)
            )

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NeonTheme.Primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = L10n.subscriptionExpiredTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = L10n.subscriptionExpiredMessage,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SettingsCardShape)
                        .background(NeonTheme.SurfaceContainer)
                        .border(1.dp, NeonTheme.Outline.copy(alpha = 0.35f), SettingsCardShape)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = L10n.close.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = NeonTheme.OnSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SettingsCardShape)
                        .background(NeonTheme.Primary)
                        .clickable(onClick = onGoToSubscription)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = L10n.goToSubscription.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = NeonTheme.Background
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionGateOverlay(
    onDismiss: () -> Unit,
    onGoToSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
        )
        SubscriptionGateSheet(
            onDismiss = onDismiss,
            onGoToSubscription = onGoToSubscription,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
