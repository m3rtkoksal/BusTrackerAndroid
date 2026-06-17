package com.mikatechnology.BusTracker.ui.driver

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.ShuttlePoolDisplay
import com.mikatechnology.BusTracker.data.model.ShuttlePoolMode
import com.mikatechnology.BusTracker.data.model.ShuttlePoolProduct
import com.mikatechnology.BusTracker.data.repository.PoolContributionException
import com.mikatechnology.BusTracker.data.repository.PoolContributionStore
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.launch

@Composable
fun PoolPaymentScreen(
    groupID: String,
    viewModel: DriverSubscriptionViewModel,
    onShowAllContributions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    val members by ShuttleStore.shared.members.collectAsState()
    val serviceMemberCount = members.count { it.role == com.mikatechnology.BusTracker.data.model.MemberRole.Passenger } + 1

    val contributionStore = remember(activity) {
        activity?.let { PoolContributionStore(it) }
    }

    DisposableEffect(contributionStore) {
        onDispose { contributionStore?.destroy() }
    }

    LaunchedEffect(contributionStore, groupID) {
        contributionStore?.loadProducts()
    }

    val payButtonLabel = contributionStore?.selectedTier?.let { tier ->
        L10n.poolPayButtonWithPrice(contributionStore.displayPrice(tier))
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PoolModeSection(
            selectedMode = viewModel.selectedMembershipMode,
            serviceMemberCount = serviceMemberCount,
            onSelectMode = viewModel::presentMembershipPurchase
        )

        PoolContributionHistorySection(
            items = viewModel.contributionHistory,
            onShowAllContributions = onShowAllContributions
        )

        if (contributionStore != null) {
            PoolTierSection(
                contributionStore = contributionStore,
                selectedTier = contributionStore.selectedTier,
                isLoading = contributionStore.isLoadingProducts,
                onSelectTier = contributionStore::selectTier,
                onRefreshPrices = {
                    scope.launch { contributionStore.loadProducts(forceRefresh = true) }
                }
            )

            PoolPayButton(
                enabled = contributionStore.selectedTier != null
                    && !contributionStore.isPurchasing
                    && !contributionStore.isLoadingProducts,
                isPurchasing = contributionStore.isPurchasing,
                label = payButtonLabel ?: L10n.poolPayButton,
                onClick = {
                    scope.launch {
                        try {
                            val result = contributionStore.purchaseSelectedTier(groupID)
                            viewModel.applyContributionResult(result)
                            viewModel.showSuccess(L10n.poolPurchaseSuccess)
                            contributionStore.clearError()
                            viewModel.load(groupID, preferServer = true)
                        } catch (error: PoolContributionException) {
                            if (error is PoolContributionException.PurchaseCancelled) return@launch
                            contributionStore.reportError(
                                error.message ?: L10n.poolPurchaseBackendFailed
                            )
                        } catch (error: Exception) {
                            contributionStore.reportError(error.message ?: L10n.poolPurchaseBackendFailed)
                        }
                    }
                }
            )
        } else {
            Text(
                text = L10n.poolPurchaseBillingUnavailable,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.Primary.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        contributionStore?.lastError?.let { error ->
            Text(
                text = error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = NeonTheme.Primary.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Text(
            text = L10n.subscriptionPaymentHint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = NeonTheme.Outline,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun PoolModeSection(
    selectedMode: ShuttlePoolMode,
    serviceMemberCount: Int,
    onSelectMode: (ShuttlePoolMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PoolSectionHeader(L10n.poolPaymentTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShuttlePoolMode.entries.forEach { mode ->
                PoolModeButton(
                    mode = mode,
                    isSelected = selectedMode == mode,
                    serviceMemberCount = serviceMemberCount,
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PoolModeButton(
    mode: ShuttlePoolMode,
    isSelected: Boolean,
    serviceMemberCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hintParts = mode.hintParts(serviceMemberCount)
    Column(
        modifier = modifier
            .clip(SettingsCardShape)
            .background(
                if (isSelected) NeonTheme.Primary.copy(alpha = 0.12f) else NeonTheme.SurfaceContainer
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = (if (isSelected) NeonTheme.Primary else NeonTheme.Outline)
                    .copy(alpha = if (isSelected) 0.5f else 0.3f),
                shape = SettingsCardShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = mode.displayTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) NeonTheme.Primary else NeonTheme.OnSurfaceVariant
        )
        Text(
            text = ShuttlePoolDisplay.formatCurrency(mode.targetAmount),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) NeonTheme.Primary else NeonTheme.OnSurface
        )
        Text(
            text = buildAnnotatedString {
                append(hintParts.leading)
                withStyle(SpanStyle(color = NeonTheme.Secondary)) {
                    append(hintParts.highlighted)
                }
                append(hintParts.trailing)
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = NeonTheme.OnSurfaceVariant
        )
    }
}

@Composable
fun PoolBalanceSection(poolCollected: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PoolSectionHeader(L10n.poolBalanceSectionTitle)
        Text(
            text = ShuttlePoolDisplay.formatCurrency(poolCollected),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = NeonTheme.Primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(NeonTheme.SurfaceContainerHigh)
                .border(1.dp, NeonTheme.Primary.copy(alpha = 0.35f))
                .padding(16.dp)
        )
    }
}

@Composable
private fun PoolContributionHistorySection(
    items: List<com.mikatechnology.BusTracker.data.model.PoolContributionHistoryItem>,
    onShowAllContributions: () -> Unit
) {
    val previewItems = items.take(3)
    val hasMore = items.size > 3

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PoolSectionHeader(L10n.poolContributionHistoryTitle)
        PoolContributionHistoryCard(items = previewItems)

        if (hasMore) {
            Text(
                text = L10n.poolShowMoreContributions.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = NeonTheme.Background,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(NeonTheme.Secondary)
                    .clickable(onClick = onShowAllContributions)
                    .padding(vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun PoolTierSection(
    contributionStore: PoolContributionStore,
    selectedTier: ShuttlePoolProduct?,
    isLoading: Boolean,
    onSelectTier: (ShuttlePoolProduct) -> Unit,
    onRefreshPrices: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PoolSectionHeader(L10n.poolSelectAmount, modifier = Modifier.weight(1f))
            if (!isLoading) {
                Text(
                    text = L10n.poolRefreshPrices,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonTheme.Primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onRefreshPrices)
                )
            }
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = NeonTheme.Primary,
                    strokeWidth = 2.dp
                )
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(
                items = ShuttlePoolProduct.entries,
                key = { it.name }
            ) { tier ->
                val isSelected = selectedTier == tier
                Text(
                    text = contributionStore.displayPrice(tier),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) NeonTheme.Background else NeonTheme.OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SettingsCardShape)
                        .background(if (isSelected) NeonTheme.Primary else NeonTheme.SurfaceContainer)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) NeonTheme.Primary else NeonTheme.Outline.copy(alpha = 0.35f),
                            shape = SettingsCardShape
                        )
                        .clickable { onSelectTier(tier) }
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun PoolPayButton(
    enabled: Boolean,
    isPurchasing: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(SettingsCardShape)
            .background(if (enabled) NeonTheme.Primary else NeonTheme.Outline)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPurchasing) {
            CircularProgressIndicator(
                color = NeonTheme.Background,
                modifier = Modifier.padding(end = 8.dp),
                strokeWidth = 2.dp
            )
        }
        Text(
            text = label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = NeonTheme.Background
        )
    }
}

@Composable
private fun PoolSectionHeader(
    title: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
) {
    Row(
        modifier = modifier,
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
