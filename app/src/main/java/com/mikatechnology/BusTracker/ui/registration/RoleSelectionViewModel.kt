package com.mikatechnology.BusTracker.ui.registration

import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavSubtitleStyle
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.localization.L10n

class RoleSelectionViewModel(
    private val onLoginTapped: () -> Unit
) : BaseViewModel() {
    init {
        configureScreen(
            title = L10n.roleSelectionTitle,
            subtitle = L10n.roleSelectionSubtitle,
            navSubtitleStyle = NavSubtitleStyle.NeonCaps,
            navigationBarStyle = NavigationBarStyle.NeonAuth,
            usesLargeTitle = false,
            hidesNavigationBar = true,
            embedsInNavigationStack = false,
            usesCustomNavHeader = true
        )
    }

    fun loginTapped() = onLoginTapped()
}
