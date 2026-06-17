package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.ShuttlePoolDisplay
import com.mikatechnology.BusTracker.data.model.ShuttlePoolMode
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun PoolMembershipPurchaseBottomSheet(
    mode: ShuttlePoolMode,
    isPurchasing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(NeonTheme.SurfaceContainer)
            .navigationBarsPadding()
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

            Text(
                text = L10n.poolPaymentTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = L10n.poolPurchaseMembershipConfirm(mode.displayTitle),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = ShuttlePoolDisplay.formatCurrency(mode.targetAmount),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = NeonTheme.Primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            errorMessage?.let { error ->
                Text(
                    text = error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTheme.Primary.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

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
                        .clickable(enabled = !isPurchasing, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = L10n.no.uppercase(),
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
                        .clickable(enabled = !isPurchasing, onClick = onConfirm)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPurchasing) {
                            CircularProgressIndicator(
                                color = NeonTheme.Background,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp)
                            )
                        }
                        Text(
                            text = L10n.yes.uppercase(),
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
}
