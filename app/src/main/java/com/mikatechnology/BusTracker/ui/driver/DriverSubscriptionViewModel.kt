package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikatechnology.BusTracker.base.PopupPresentation
import com.mikatechnology.BusTracker.base.PopupStyle
import com.mikatechnology.BusTracker.data.model.DriverSubscriptionDisplay
import com.mikatechnology.BusTracker.data.model.DriverSubscriptionInfo
import com.mikatechnology.BusTracker.data.model.PoolContributionResult
import com.mikatechnology.BusTracker.data.model.ShuttlePoolDisplay
import com.mikatechnology.BusTracker.data.model.ShuttlePoolMode
import com.mikatechnology.BusTracker.data.model.ShuttlePoolState
import com.mikatechnology.BusTracker.data.repository.DriverSubscriptionService
import com.mikatechnology.BusTracker.data.repository.PoolMembershipException
import com.mikatechnology.BusTracker.localization.L10n

class DriverSubscriptionViewModel {
    var poolState by mutableStateOf(ShuttlePoolState.Empty)
        private set
    var contributionHistory by mutableStateOf<List<com.mikatechnology.BusTracker.data.model.PoolContributionHistoryItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var toast by mutableStateOf<PopupPresentation?>(null)
        private set
    var selectedMembershipMode by mutableStateOf(ShuttlePoolMode.Monthly)
        private set
    var pendingMembershipMode by mutableStateOf<ShuttlePoolMode?>(null)
        private set
    var isPurchasingMembership by mutableStateOf(false)
        private set
    var membershipPurchaseError by mutableStateOf<String?>(null)
        private set

    val subscription: DriverSubscriptionInfo
        get() = poolState.subscription

    val startDateText: String
        get() = DriverSubscriptionDisplay.formatDate(poolState.subscription.startDate)

    val endDateText: String
        get() = DriverSubscriptionDisplay.formatDate(poolState.subscription.endDate)

    val statusSubtitle: String
        get() {
            poolState.subscription.daysRemaining?.takeIf { poolState.subscription.isExpiringSoon }?.let { days ->
                return L10n.subscriptionExpiringSoonSubtitle(days)
            }
            if (poolState.isServiceOperational) return endDateText
            if (poolState.poolCollected > 0) {
                return ShuttlePoolDisplay.formatCurrency(poolState.poolCollected)
            }
            return L10n.subscriptionInactive
        }

    val expiringSoonMessage: String?
        get() {
            val days = poolState.subscription.daysRemaining ?: return null
            if (!poolState.subscription.isExpiringSoon) return null
            return L10n.subscriptionExpiringSoonMessage(endDateText, days)
        }

    val isExpiringSoon: Boolean
        get() = poolState.subscription.isExpiringSoon

    suspend fun load(groupID: String, preferServer: Boolean = false) {
        if (groupID.isBlank()) {
            poolState = ShuttlePoolState.Empty
            return
        }
        isLoading = true
        try {
            poolState = DriverSubscriptionService.fetchPoolState(groupID, preferServer)
            contributionHistory = DriverSubscriptionService.fetchContributionHistory(groupID)
            if (pendingMembershipMode == null) {
                selectedMembershipMode = poolState.poolMode
            }
        } finally {
            isLoading = false
        }
    }

    fun applyContributionResult(result: PoolContributionResult) {
        poolState = poolState.copy(
            poolCollected = result.poolCollected,
            poolTarget = result.poolTarget
        )
    }

    fun showSuccess(message: String) {
        toast = PopupPresentation(
            style = PopupStyle.Success,
            title = L10n.success,
            message = message
        )
    }

    fun clearToast() {
        toast = null
    }

    suspend fun purchaseMembership(mode: ShuttlePoolMode, groupID: String) {
        val result = DriverSubscriptionService.purchaseMembership(groupID, mode)
        poolState = poolState.copy(
            poolCollected = result.poolCollected,
            poolTarget = result.poolTarget,
            poolMode = mode
        )
    }

    fun presentMembershipPurchase(mode: ShuttlePoolMode) {
        selectedMembershipMode = mode
        pendingMembershipMode = mode
        membershipPurchaseError = null
    }

    fun dismissMembershipPurchase() {
        pendingMembershipMode = null
        membershipPurchaseError = null
        isPurchasingMembership = false
    }

    suspend fun confirmMembershipPurchase(mode: ShuttlePoolMode, groupID: String) {
        isPurchasingMembership = true
        membershipPurchaseError = null
        try {
            purchaseMembership(mode, groupID)
            selectedMembershipMode = mode
            showSuccess(L10n.poolMembershipPurchaseSuccess)
            dismissMembershipPurchase()
            load(groupID, preferServer = true)
        } catch (error: PoolMembershipException) {
            membershipPurchaseError = error.message
        } catch (error: Exception) {
            membershipPurchaseError = error.message ?: L10n.poolPurchaseBackendFailed
        } finally {
            isPurchasingMembership = false
        }
    }
}
