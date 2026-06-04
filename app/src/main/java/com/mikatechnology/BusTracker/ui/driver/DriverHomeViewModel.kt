package com.mikatechnology.BusTracker.ui.driver

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.ShuttleMember
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DriverHomeViewModel(
    private val profile: UserProfile
) : BaseViewModel() {

    private val shuttleStore = ShuttleStore.shared
    private var isTripBusy = false

    private val _showTripDurationSheet = MutableStateFlow(false)
    val showTripDurationSheet: StateFlow<Boolean> = _showTripDurationSheet.asStateFlow()

    private val _selectedTripDurationHours = MutableStateFlow(2.0)
    val selectedTripDurationHours: StateFlow<Double> = _selectedTripDurationHours.asStateFlow()

    val userProfile: UserProfile
        get() = profile

    init {
        configureScreen(
            title = profile.groupName,
            navigationBarStyle = NavigationBarStyle.NeonDriver,
            hidesNavigationBar = true,
            usesLargeTitle = false,
            embedsInNavigationStack = false,
            contentScrollEnabled = false
        )
    }

    fun onAppear(groupID: String) {
        shuttleStore.startListening(groupID)
        viewModelScope.launch {
            shuttleStore.reconcileActiveTripIfExpired(profile.groupID, profile.name)
        }
    }

    fun dismissTripDurationSheet() {
        _showTripDurationSheet.value = false
    }

    fun selectTripDurationHours(hours: Double) {
        _selectedTripDurationHours.value = hours
    }

    fun passengerStats(members: List<ShuttleMember>): DriverPassengerStats {
        val passengers = members.filter { it.role == MemberRole.Passenger }
        return DriverPassengerStats(
            total = 15,
            coming = passengers.count { shuttleStore.serviceDayAttendanceFor(it) == AttendanceStatus.Coming },
            notComing = passengers.count { shuttleStore.serviceDayAttendanceFor(it) == AttendanceStatus.NotComing },
            unknown = passengers.count { shuttleStore.serviceDayAttendanceFor(it) == AttendanceStatus.Unknown }
        )
    }

    fun passengerMorningPickups(
        passengers: List<ShuttleMember>,
        morningPickups: List<MorningPickup>
    ): List<MorningPickup> {
        val passengerIDs = passengers.map { it.id }.toSet()

        return morningPickups.filter { pickup ->
            pickup.memberID in passengerIDs &&
                passengers.find { it.id == pickup.memberID }
                    ?.let { shuttleStore.serviceDayAttendanceFor(it) } != AttendanceStatus.NotComing
        }
    }

    fun requestSignOut(onConfirm: () -> Unit) {
        showConfirm(
            title = L10n.signOut,
            message = L10n.signOutConfirmMessage,
            confirmTitle = L10n.signOut,
            destructive = true,
            onConfirm = onConfirm
        )
    }

    fun handleTripControlTap(canStartTrip: Boolean) {
        if (isTripBusy) return
        if (shuttleStore.isTripActive.value) {
            stopTrip()
            return
        }
        if (!canStartTrip) {
            showError(L10n.alwaysLocationRequiredToStart)
            return
        }
        _showTripDurationSheet.value = true
    }

    fun confirmStartTrip(canStartTrip: Boolean) {
        if (isTripBusy) return
        if (!canStartTrip) {
            showError(L10n.alwaysLocationRequiredToStart)
            return
        }
        viewModelScope.launch {
            isTripBusy = true
            _showTripDurationSheet.value = false
            setLoading(true, L10n.startingShuttle)
            try {
                val hours = _selectedTripDurationHours.value
                shuttleStore.startTrip(profile.groupID, profile.name, hours)
                val hoursLabel = if (hours == hours.toLong().toDouble()) {
                    "${hours.toInt()} saat"
                } else {
                    "$hours saat"
                }
                showSuccess(L10n.shuttleStartedAutoStop(hoursLabel))
            } catch (error: Exception) {
                showError(error.message ?: L10n.shuttleStartFailed)
            } finally {
                isTripBusy = false
                setLoading(false)
            }
        }
    }

    private fun stopTrip() {
        viewModelScope.launch {
            isTripBusy = true
            setLoading(true, L10n.stoppingShuttle)
            try {
                shuttleStore.stopTrip(profile.groupID, profile.name)
                showSuccess(L10n.shuttleStopped)
            } catch (error: Exception) {
                showError(error.message ?: L10n.shuttleStopFailed)
            } finally {
                isTripBusy = false
                setLoading(false)
            }
        }
    }

    fun requestDeleteAccount(onConfirm: () -> Unit) {
        showConfirm(
            title = L10n.deleteAccount,
            message = L10n.deleteAccountConfirmMessage,
            confirmTitle = L10n.deleteAccountPermanently,
            destructive = true,
            onConfirm = onConfirm
        )
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            setLoading(true, L10n.signingOut)
            try {
                if (shuttleStore.isTripActive.value) {
                    shuttleStore.stopTrip(profile.groupID, profile.name)
                }
                shuttleStore.stopListening()
                UserSessionRepository.signOut(context)
            } catch (error: Exception) {
                showError(error.message ?: L10n.signOutFailed)
            } finally {
                setLoading(false)
            }
        }
    }

    fun deleteAccount(context: Context, googleReauthData: Intent?) {
        viewModelScope.launch {
            if (googleReauthData == null) {
                showError(L10n.googleVerificationRequiredForDelete)
                return@launch
            }

            setLoading(true, L10n.deletingAccount)
            var profileDeleted = false
            var authRemoved = false
            var shouldSignOut = false
            try {
                if (!AuthRepository.reauthenticateWithGoogle(googleReauthData)) {
                    showError(L10n.googleVerificationFailed)
                    return@launch
                }

                if (shuttleStore.isTripActive.value) {
                    shuttleStore.stopTrip(profile.groupID, profile.name)
                }
                shuttleStore.deleteUserData(profile)
                profileDeleted = shuttleStore.isUserProfileDeleted(profile.userID)
                authRemoved = AuthRepository.tryDeleteAuthUser() == AuthRepository.AuthDeleteStep.Deleted
                shouldSignOut = true
            } catch (_: Exception) {
                shouldSignOut = profileDeleted || authRemoved
            } finally {
                shuttleStore.stopListening()
                if (shouldSignOut) {
                    UserSessionRepository.signOut(context)
                }
                setLoading(false)
                if (!shouldSignOut) return@launch
                if (profileDeleted || authRemoved) {
                    showSuccess(L10n.accountDeletedSuccess)
                } else {
                    showError(L10n.accountDeleteFailed)
                }
            }
        }
    }

}

class DriverHomeViewModelFactory(
    private val profile: UserProfile
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverHomeViewModel::class.java)) {
            return DriverHomeViewModel(profile) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
