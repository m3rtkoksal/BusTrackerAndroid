package com.mikatechnology.BusTracker.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mikatechnology.BusTracker.data.model.MemberRole

class RegistrationFormViewModelFactory(
    private val role: MemberRole
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RegistrationFormViewModel(role = role) as T
    }
}
