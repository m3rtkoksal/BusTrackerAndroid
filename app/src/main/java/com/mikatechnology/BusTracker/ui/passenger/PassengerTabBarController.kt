package com.mikatechnology.BusTracker.ui.passenger

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PassengerTabBarController : ViewModel() {
    private val _selectedTab = MutableStateFlow(PassengerHomeTab.Map)
    val selectedTab: StateFlow<PassengerHomeTab> = _selectedTab.asStateFlow()

    fun select(tab: PassengerHomeTab) {
        _selectedTab.value = tab
    }
}
