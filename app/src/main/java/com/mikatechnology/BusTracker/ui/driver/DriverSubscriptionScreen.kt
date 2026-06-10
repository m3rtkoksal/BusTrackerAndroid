package com.mikatechnology.BusTracker.ui.driver

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.repository.DriverSubscriptionShare
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.CopyServiceCode
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.settings.SettingsInfoRow
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun DriverSubscriptionScreen(
    groupID: String,
    serviceCode: String,
    viewModel: DriverSubscriptionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renewalUrl = DriverSubscriptionShare.renewalUrl(serviceCode)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeonTheme.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubscriptionBackButton(onClick = onBack)
                Text(
                    text = L10n.subscription,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonTheme.OnSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = L10n.subscriptionSectionTitle.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NeonTheme.Primary
                )
                Text(
                    text = if (viewModel.subscription.isActive) {
                        L10n.subscriptionActiveDescription
                    } else {
                        L10n.subscriptionInactiveDescription
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant
                )
                if (!viewModel.subscription.isActive) {
                    Text(
                        text = L10n.subscriptionBossPaymentHint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonTheme.Outline
                    )
                } else {
                    viewModel.expiringSoonMessage?.let { message ->
                        SubscriptionExpiringSoonBanner(message = message)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsInfoRow(
                    title = L10n.subscriptionStartDate,
                    value = viewModel.startDateText
                )
                SettingsInfoRow(
                    title = L10n.subscriptionEndDate,
                    value = viewModel.endDateText
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = L10n.subscriptionPaymentLinkTitle.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NeonTheme.Primary
                )

                Text(
                    text = L10n.subscriptionRenewalHint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonTheme.OnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubscriptionLinkActionButton(
                        title = L10n.share,
                        icon = Icons.Default.Share,
                        accent = NeonTheme.Primary,
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, DriverSubscriptionShare.shareMessage(serviceCode))
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        }
                    )
                    SubscriptionLinkActionButton(
                        title = L10n.copy,
                        icon = Icons.Default.ContentCopy,
                        accent = NeonTheme.OnSurfaceVariant,
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val copied = CopyServiceCode.copyPlainText(context, renewalUrl)
                            CopyServiceCode.showPlainCopyResult(context, copied, L10n.subscriptionLinkCopied)
                        }
                    )
                }
            }

            Text(
                text = L10n.subscriptionPaymentHint,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.Outline,
                textAlign = TextAlign.Center
            )
        }

        if (viewModel.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = NeonTheme.Secondary
            )
        }
    }
}

@Composable
private fun SubscriptionLinkActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(SettingsCardShape)
            .background(if (filled) NeonTheme.Primary.copy(alpha = 0.1f) else NeonTheme.SurfaceContainer)
            .border(1.dp, accent.copy(alpha = if (filled) 0.4f else 0.3f), SettingsCardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = accent
        )
    }
}

@Composable
private fun SubscriptionBackButton(onClick: () -> Unit) {
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
