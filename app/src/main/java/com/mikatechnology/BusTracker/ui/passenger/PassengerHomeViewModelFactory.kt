package com.mikatechnology.BusTracker.ui.passenger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mikatechnology.BusTracker.data.model.UserProfile

class PassengerHomeViewModelFactory(
    private val profile: UserProfile
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PassengerHomeViewModel::class.java)) {
            return PassengerHomeViewModel(profile) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
