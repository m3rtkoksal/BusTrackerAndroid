package com.mikatechnology.BusTracker.data.model

import com.mikatechnology.BusTracker.localization.L10n

data class PoolModeHintParts(
    val leading: String,
    val highlighted: String,
    val trailing: String
)

enum class ShuttlePoolMode(val rawValue: String) {
    Weekly("weekly"),
    Monthly("monthly"),
    Annual("annual");

    val targetAmount: Int
        get() = when (this) {
            Weekly -> ShuttlePoolConfig.weeklyTarget
            Monthly -> ShuttlePoolConfig.monthlyTarget
            Annual -> ShuttlePoolConfig.annualTarget
        }

    val extensionDays: Int
        get() = when (this) {
            Weekly -> 7
            Monthly -> 30
            Annual -> 365
        }

    val displayTitle: String
        get() = when (this) {
            Weekly -> L10n.poolModeWeekly
            Monthly -> L10n.poolModeMonthly
            Annual -> L10n.poolModeAnnual
        }

    fun hintParts(memberCount: Int): PoolModeHintParts = when (this) {
        Weekly -> L10n.poolWeeklyHintParts(memberCount)
        Monthly -> L10n.poolMonthlyHintParts(memberCount)
        Annual -> L10n.poolAnnualHintParts(memberCount)
    }

    companion object {
        fun fromRaw(value: String?): ShuttlePoolMode =
            entries.firstOrNull { it.rawValue == value } ?: Monthly
    }
}

object ShuttlePoolConfig {
    const val weeklyTarget = 50
    const val monthlyTarget = 99
    const val annualTarget = 999
    const val expiringSoonDays = 2
    val contributionTiers = listOf(50, 100, 250, 500, 1000)
}

object ShuttlePoolBilling {
    const val PLAY_PRODUCT_ID = "com.mikatechnology.bustracker.pool"
}

enum class ShuttlePoolProduct(val amount: Int) {
    Tier50(50),
    Tier100(100),
    Tier250(250),
    Tier500(500),
    Tier1000(1000);

    val purchaseOptionId: String
        get() = "tier-$amount"

    /** Firestore / backend + iOS parity (`…pool.50`). */
    val backendProductId: String
        get() = "${ShuttlePoolBilling.PLAY_PRODUCT_ID}.$amount"

    companion object {
        fun matching(amount: Int): ShuttlePoolProduct? =
            entries.firstOrNull { it.amount == amount }
    }
}

data class PoolContributionResult(
    val poolCollected: Int,
    val poolTarget: Int,
    val activated: Boolean
)

data class PoolContributionHistoryItem(
    val id: String,
    val memberName: String,
    val amount: Int
) {
    val amountText: String
        get() = ShuttlePoolDisplay.formatCurrency(amount)
}

data class ShuttlePoolState(
    val subscription: DriverSubscriptionInfo = DriverSubscriptionInfo.Empty,
    val poolMode: ShuttlePoolMode = ShuttlePoolMode.Monthly,
    val poolTarget: Int = ShuttlePoolConfig.monthlyTarget,
    val poolCollected: Int = 0
) {
    val remainingBalance: Int
        get() = maxOf(0, poolTarget - poolCollected)

    val isPoolComplete: Boolean
        get() = poolCollected >= poolTarget

    val isServiceOperational: Boolean
        get() = subscription.isServiceOperational

    val hasFullFeatures: Boolean
        get() = subscription.hasFullFeatures

    val capabilities: ShuttleCapabilities
        get() = ShuttleCapabilities.from(subscription)

    companion object {
        val Empty = ShuttlePoolState()
    }
}

object ShuttlePoolDisplay {
    fun formatCurrency(amount: Int): String = L10n.poolCurrency(amount)
}
