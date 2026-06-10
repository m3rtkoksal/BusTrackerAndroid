package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikatechnology.BusTracker.data.model.DriverSubscriptionDisplay
import com.mikatechnology.BusTracker.data.model.DriverSubscriptionInfo
import com.mikatechnology.BusTracker.data.repository.DriverSubscriptionService
import com.mikatechnology.BusTracker.localization.L10n

class DriverSubscriptionViewModel {
    var subscription by mutableStateOf(DriverSubscriptionInfo.Empty)
        private set
    var isLoading by mutableStateOf(false)
        private set

    val startDateText: String
        get() = DriverSubscriptionDisplay.formatDate(subscription.startDate)

    val endDateText: String
        get() = DriverSubscriptionDisplay.formatDate(subscription.endDate)

    val statusSubtitle: String
        get() {
            subscription.daysRemaining?.takeIf { subscription.isExpiringSoon }?.let { days ->
                return L10n.subscriptionExpiringSoonSubtitle(days)
            }
            return if (subscription.isActive) endDateText else L10n.subscriptionInactive
        }

    val expiringSoonMessage: String?
        get() {
            val days = subscription.daysRemaining ?: return null
            if (!subscription.isExpiringSoon) return null
            return L10n.subscriptionExpiringSoonMessage(endDateText, days)
        }

    val isExpiringSoon: Boolean
        get() = subscription.isExpiringSoon

    suspend fun load(groupID: String) {
        if (groupID.isBlank()) {
            subscription = DriverSubscriptionInfo.Empty
            return
        }
        isLoading = true
        try {
            subscription = DriverSubscriptionService.fetchSubscription(groupID)
        } finally {
            isLoading = false
        }
    }
}
