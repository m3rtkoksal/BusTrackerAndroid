package com.mikatechnology.BusTracker.data.model

/** Feature access level based on subscription status. */
enum class ShuttleFeatureLevel {
    Full,
    Minimal
}

/** Tracks which features are available based on subscription. */
data class ShuttleCapabilities(
    val featureLevel: ShuttleFeatureLevel,
    val daysRemaining: Int?,
    val isExpiringSoon: Boolean
) {
    val hasFullFeatures: Boolean
        get() = featureLevel == ShuttleFeatureLevel.Full

    // Features that work in minimal mode
    val canStartTrip: Boolean = true
    val canStopTrip: Boolean = true
    val canViewMap: Boolean = true

    // Features that require full subscription
    val canSetAttendance: Boolean
        get() = hasFullFeatures
    val canSetHolidayMode: Boolean
        get() = hasFullFeatures
    val canSavePickupLocation: Boolean
        get() = hasFullFeatures
    val canReceiveBoardingNotifications: Boolean
        get() = hasFullFeatures
    val canReceiveApproachNotifications: Boolean
        get() = hasFullFeatures
    val canSendDelayNotice: Boolean
        get() = hasFullFeatures

    companion object {
        val Full = ShuttleCapabilities(
            featureLevel = ShuttleFeatureLevel.Full,
            daysRemaining = null,
            isExpiringSoon = false
        )

        val Minimal = ShuttleCapabilities(
            featureLevel = ShuttleFeatureLevel.Minimal,
            daysRemaining = null,
            isExpiringSoon = false
        )

        fun from(subscription: DriverSubscriptionInfo): ShuttleCapabilities {
            return ShuttleCapabilities(
                featureLevel = if (subscription.hasFullFeatures) ShuttleFeatureLevel.Full else ShuttleFeatureLevel.Minimal,
                daysRemaining = subscription.daysRemaining,
                isExpiringSoon = subscription.isExpiringSoon
            )
        }
    }
}
