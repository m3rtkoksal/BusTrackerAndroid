package com.mikatechnology.BusTracker.base

import java.util.UUID

enum class NavigationBarStyle {
    Primary,
    Auth,
    NeonAuth,
    NeonDriver,
    NeonPassenger,
    Driver,
    Passenger;

    val usesTransparentBar: Boolean
        get() = when (this) {
            Driver, Passenger -> false
            Primary, Auth, NeonAuth, NeonDriver, NeonPassenger -> true
        }

    val usesCustomNavHeader: Boolean
        get() = this == NeonAuth

    val usesNeonBackground: Boolean
        get() = this == NeonAuth || this == NeonDriver || this == NeonPassenger

    val usesDarkToolbar: Boolean
        get() = when (this) {
            Driver, Passenger, NeonAuth, NeonDriver, NeonPassenger -> true
            Primary, Auth -> false
        }
}

enum class NavSubtitleStyle {
    Hidden,
    Standard,
    NeonCaps
}

enum class PopupStyle {
    Error,
    Success,
    Info,
    Warning
}

data class PopupPresentation(
    val id: String = UUID.randomUUID().toString(),
    val style: PopupStyle,
    val title: String,
    val message: String
)

data class ConfirmPresentation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val confirmTitle: String,
    val cancelTitle: String,
    val isDestructive: Boolean = false
)

data class BaseUiState(
    val title: String = "",
    val subtitle: String = "",
    val navSubtitleStyle: NavSubtitleStyle = NavSubtitleStyle.Hidden,
    val navigationBarStyle: NavigationBarStyle = NavigationBarStyle.Primary,
    val showsBackButton: Boolean = false,
    val usesLargeTitle: Boolean = true,
    val hidesNavigationBar: Boolean = false,
    val embedsInNavigationStack: Boolean = true,
    val usesCustomNavHeader: Boolean? = null,
    val contentScrollEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val loadingMessage: String = "Yükleniyor...",
    val alert: PopupPresentation? = null,
    val toast: PopupPresentation? = null,
    val confirmDialog: ConfirmPresentation? = null
)
