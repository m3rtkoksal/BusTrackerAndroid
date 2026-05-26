package com.mikatechnology.BusTracker.ui.registration

import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavSubtitleStyle
import com.mikatechnology.BusTracker.base.NavigationBarStyle

class RoleSelectionViewModel(
    private val onLoginTapped: () -> Unit
) : BaseViewModel() {
    init {
        configureScreen(
            title = "Hesap Oluştur",
            subtitle = "Sürücü müsünüz, yolcu mu?",
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
