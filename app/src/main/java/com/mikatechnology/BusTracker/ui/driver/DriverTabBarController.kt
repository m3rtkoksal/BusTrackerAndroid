package com.mikatechnology.BusTracker.ui.driver

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DriverTabBarController : ViewModel() {
    private val _selectedTab = MutableStateFlow(DriverHomeTab.Passengers)
    val selectedTab: StateFlow<DriverHomeTab> = _selectedTab.asStateFlow()

    fun select(tab: DriverHomeTab) {
        _selectedTab.value = tab
    }
}
