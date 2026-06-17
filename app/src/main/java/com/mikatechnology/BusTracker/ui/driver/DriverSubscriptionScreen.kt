package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mikatechnology.BusTracker.base.PopupPresentation
import com.mikatechnology.BusTracker.base.PopupStyle
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DriverSubscriptionScreen(
    groupID: String,
    viewModel: DriverSubscriptionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    LaunchedEffect(groupID) {
        viewModel.load(groupID)
    }

    LaunchedEffect(viewModel.toast?.id) {
        if (viewModel.toast != null) {
            delay(3000)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeonTheme.Background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "subscription_main",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("subscription_main") {
                SubscriptionMainContent(
                    groupID = groupID,
                    viewModel = viewModel,
                    onBack = onBack,
                    onShowAllContributions = { navController.navigate("pool_contribution_history") }
                )
            }
            composable("pool_contribution_history") {
                PoolContributionHistoryListScreen(
                    items = viewModel.contributionHistory,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        AnimatedVisibility(
            visible = viewModel.toast != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .zIndex(1f)
        ) {
            viewModel.toast?.let { toast ->
                SubscriptionToastBanner(
                    popup = toast,
                    onDismiss = viewModel::clearToast
                )
            }
        }

        if (viewModel.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = NeonTheme.Secondary
            )
        }

        AnimatedVisibility(
            visible = viewModel.pendingMembershipMode != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(3f)
        ) {
            val mode = viewModel.pendingMembershipMode ?: return@AnimatedVisibility
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { viewModel.dismissMembershipPurchase() }
                )
                PoolMembershipPurchaseBottomSheet(
                    mode = mode,
                    isPurchasing = viewModel.isPurchasingMembership,
                    errorMessage = viewModel.membershipPurchaseError,
                    onConfirm = {
                        scope.launch {
                            viewModel.confirmMembershipPurchase(mode, groupID)
                        }
                    },
                    onDismiss = viewModel::dismissMembershipPurchase,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun SubscriptionMainContent(
    groupID: String,
    viewModel: DriverSubscriptionViewModel,
    onBack: () -> Unit,
    onShowAllContributions: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubscriptionTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SubscriptionTitleSection()
            SubscriptionStatusSection(viewModel = viewModel)
            SubscriptionDatesSection(viewModel = viewModel)
            PoolBalanceSection(poolCollected = viewModel.poolState.poolCollected)
            PoolPaymentScreen(
                groupID = groupID,
                viewModel = viewModel,
                onShowAllContributions = onShowAllContributions
            )
        }
    }
}

@Composable
private fun SubscriptionTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonTheme.Surface.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubscriptionBackButton(onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NeonTheme.Primary.copy(alpha = 0.2f))
    )
}

@Composable
private fun SubscriptionTitleSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = L10n.subscription,
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
}

@Composable
private fun SubscriptionStatusSection(viewModel: DriverSubscriptionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        viewModel.expiringSoonMessage?.let { message ->
            SubscriptionExpiringSoonBanner(
                message = message,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        val description = when {
            viewModel.poolState.hasFullFeatures -> L10n.subscriptionActiveDescription
            else -> L10n.subscriptionInactiveDescription
        }

        Text(
            text = description,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = NeonTheme.OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun SubscriptionDatesSection(viewModel: DriverSubscriptionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubscriptionSectionHeader(L10n.subscriptionSectionTitle)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MembershipDateRow(L10n.subscriptionStartDate, viewModel.startDateText)
            MembershipDateRow(L10n.subscriptionEndDate, viewModel.endDateText)
        }
    }
}

@Composable
private fun SubscriptionSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(NeonTheme.Primary)
        )
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color = NeonTheme.OnSurfaceVariant
        )
    }
}

@Composable
private fun MembershipDateRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonTheme.SurfaceContainer)
            .border(1.dp, NeonTheme.OnSurface.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            color = NeonTheme.OnSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = NeonTheme.OnSurface
        )
    }
}

@Composable
private fun SubscriptionToastBanner(
    popup: PopupPresentation,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(SettingsCardShape)
            .background(NeonTheme.SurfaceContainer.copy(alpha = 0.95f))
            .border(1.dp, NeonTheme.Primary.copy(alpha = 0.35f), SettingsCardShape)
            .clickable(onClick = onDismiss)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PopupIcon(popup.style)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            if (popup.title.isNotBlank()) {
                Text(
                    text = popup.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTheme.OnSurface
                )
            }
            Text(
                text = popup.message,
                fontSize = 12.sp,
                color = NeonTheme.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun PopupIcon(style: PopupStyle) {
    val tint = when (style) {
        PopupStyle.Success -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        else -> NeonTheme.Primary
    }
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = tint
    )
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
