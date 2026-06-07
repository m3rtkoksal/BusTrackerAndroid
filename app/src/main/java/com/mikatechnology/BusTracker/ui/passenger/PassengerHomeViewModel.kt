package com.mikatechnology.BusTracker.ui.passenger

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.HolidayMode
import com.mikatechnology.BusTracker.data.model.isHolidayModeActive
import com.mikatechnology.BusTracker.data.model.ServiceSchedule
import com.mikatechnology.BusTracker.data.model.UpcomingService
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.services.AttendanceUsageTracker
import com.mikatechnology.BusTracker.services.BusTrackerAnalytics
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class PassengerHomeViewModel(
    private val profile: UserProfile
) : BaseViewModel() {

    private val store = ShuttleStore.shared

    private val _showTripStartedAttendanceSheet = MutableStateFlow(false)
    val showTripStartedAttendanceSheet: StateFlow<Boolean> = _showTripStartedAttendanceSheet.asStateFlow()

    private val _pendingAttendanceStatus = MutableStateFlow<AttendanceStatus?>(null)
    val pendingAttendanceStatus: StateFlow<AttendanceStatus?> = _pendingAttendanceStatus.asStateFlow()

    private var didPromptAttendanceThisTrip = false

    private val _isUpdatingAttendance = MutableStateFlow(false)
    val isUpdatingAttendance: StateFlow<Boolean> = _isUpdatingAttendance.asStateFlow()

    private val _isSavingPickup = MutableStateFlow(false)
    val isSavingPickup: StateFlow<Boolean> = _isSavingPickup.asStateFlow()

    private val _isSavingHolidayMode = MutableStateFlow(false)
    val isSavingHolidayMode: StateFlow<Boolean> = _isSavingHolidayMode.asStateFlow()

    private val _draftPickupCoordinate = MutableStateFlow<com.google.android.gms.maps.model.LatLng?>(null)
    val draftPickupCoordinate: StateFlow<com.google.android.gms.maps.model.LatLng?> = _draftPickupCoordinate.asStateFlow()

    val userProfile: UserProfile get() = profile

    // MARK: - Computed Properties

    val members: StateFlow<List<com.mikatechnology.BusTracker.data.model.ShuttleMember>> = store.members

    val myMember: com.mikatechnology.BusTracker.data.model.ShuttleMember?
        get() = store.members.value.firstOrNull { it.id == profile.memberID }

    val isHolidayModeActive: Boolean
        get() = myMember?.isHolidayModeActive() == true

    val nextTwoServices: List<UpcomingService>
        get() = ServiceSchedule.nextTwoServices()

    val currentDriverService: UpcomingService
        get() = ServiceSchedule.currentDriverSession()

    fun rawAttendance(service: UpcomingService): AttendanceStatus =
        store.rawAttendanceFor(profile.memberID, service.dateKey)

    fun effectiveAttendance(service: UpcomingService): AttendanceStatus =
        store.effectiveAttendanceFor(profile.memberID, service.dateKey)

    val currentServiceRawAttendance: AttendanceStatus
        get() = rawAttendance(currentDriverService)

    val currentServiceEffectiveAttendance: AttendanceStatus
        get() = effectiveAttendance(currentDriverService)

    fun isComingSelected(service: UpcomingService): Boolean =
        rawAttendance(service) == AttendanceStatus.Coming

    fun isNotComingSelected(service: UpcomingService): Boolean {
        val raw = rawAttendance(service)
        val effective = effectiveAttendance(service)
        return raw == AttendanceStatus.NotComing ||
            (isHolidayModeActive && raw == AttendanceStatus.Unknown && effective == AttendanceStatus.NotComing)
    }

    val savedMorningPickup: com.mikatechnology.BusTracker.data.model.MorningPickup?
        get() = store.morningPickup(profile.memberID)

    val hasSavedMorningPickup: Boolean
        get() = savedMorningPickup != null

    val isBoardedToday: Boolean
        get() = myMember?.isBoardedToday == true

    val holidayModeSubtitle: String
        get() = if (isHolidayModeActive) {
            myMember?.holidayModeEndDate
                ?.let { HolidayMode.displayDate(it) }
                ?.let { L10n.holidayModeUntil(it) }
                ?: L10n.holidayModeOff
        } else {
            L10n.holidayModeOff
        }

    val holidayModeDetailLine: String
        get() = if (isHolidayModeActive) {
            myMember?.holidayModeEndDate
                ?.let { HolidayMode.displayDate(it) }
                ?.let { L10n.holidayModeCardDetailActive(it) }
                ?: L10n.holidayModeCardDetailOff
        } else {
            L10n.holidayModeCardDetailOff
        }

    /** Sonraki 2 servis için attendance state listesi */
    fun getNextTwoServicesData(): List<ServiceAttendanceState> =
        nextTwoServices.map { service ->
            ServiceAttendanceState(
                service = service,
                rawAttendance = rawAttendance(service),
                effectiveAttendance = effectiveAttendance(service)
            )
        }

    init {
        configureScreen(
            title = profile.groupName,
            navigationBarStyle = NavigationBarStyle.NeonPassenger,
            hidesNavigationBar = true,
            usesLargeTitle = false,
            embedsInNavigationStack = false,
            contentScrollEnabled = false
        )
    }

    fun onAppear(groupID: String) {
        val resolvedGroupID = groupID.ifBlank { profile.primaryGroupID }
        if (resolvedGroupID.isBlank()) {
            showError(L10n.shuttleInfoNotFoundRejoin)
            return
        }
        store.startListening(resolvedGroupID)
        loadSavedPickup()
    }

    fun onTripActiveChanged(
        wasActive: Boolean,
        isActive: Boolean,
        attendance: AttendanceStatus,
        holidayModeActive: Boolean
    ) {
        if (!isActive) {
            didPromptAttendanceThisTrip = false
            _showTripStartedAttendanceSheet.value = false
            return
        }
        if (isActive && !wasActive) {
            syncTripAttendanceState(
                isTripActive = true,
                holidayModeActive = holidayModeActive,
                rawAttendance = attendance
            )
        }
    }

    fun presentTripAttendanceSheetIfNeeded(
        isTripActive: Boolean,
        attendance: AttendanceStatus,
        holidayModeActive: Boolean
    ) {
        if (!isTripActive || holidayModeActive || attendance != AttendanceStatus.Unknown || didPromptAttendanceThisTrip) {
            return
        }
        didPromptAttendanceThisTrip = true
        _showTripStartedAttendanceSheet.value = true
    }

    fun syncTripAttendanceState(
        isTripActive: Boolean,
        holidayModeActive: Boolean,
        rawAttendance: AttendanceStatus
    ) {
        if (!isTripActive) return
        presentTripAttendanceSheetIfNeeded(isTripActive, rawAttendance, holidayModeActive)
    }

    fun dismissTripAttendanceSheet() {
        _showTripStartedAttendanceSheet.value = false
    }

    fun selectDraftCoordinate(latLng: com.google.android.gms.maps.model.LatLng) {
        _draftPickupCoordinate.value = latLng
    }

    fun clearDraftCoordinate() {
        _draftPickupCoordinate.value = null
    }

    private fun loadSavedPickup() {
        // This function is kept for compatibility but no longer sets draftPickupCoordinate.
        // The saved pickup is shown directly from savedMorningPickup,
        // draftPickupCoordinate is only for NEW locations selected by the user.
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

    fun requestDeleteAccount(onConfirm: () -> Unit) {
        showConfirm(
            title = L10n.deleteAccount,
            message = L10n.deleteAccountConfirmMessagePassenger,
            confirmTitle = L10n.deleteAccountPermanently,
            destructive = true,
            onConfirm = onConfirm
        )
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            setLoading(true, L10n.signingOut)
            try {
                store.stopListening()
                UserSessionRepository.signOut(context)
            } catch (e: Exception) {
                showError(e.message ?: L10n.signOutFailed)
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

                store.deleteUserData(profile)
                profileDeleted = store.isUserProfileDeleted(profile.userID)
                authRemoved = AuthRepository.tryDeleteAuthUser() == AuthRepository.AuthDeleteStep.Deleted
                shouldSignOut = true
            } catch (_: Exception) {
                shouldSignOut = profileDeleted || authRemoved
            } finally {
                store.stopListening()
                if (shouldSignOut) {
                    UserSessionRepository.signOut(context)
                }
                setLoading(false)
                if (!shouldSignOut) return@launch
                if (profileDeleted && authRemoved) {
                    showSuccess(L10n.accountDeletedSuccess)
                } else if (profileDeleted || authRemoved) {
                    showSuccess(L10n.accountDeletedSuccess)
                } else {
                    showError(L10n.accountDeleteFailed)
                }
            }
        }
    }

    private fun resolvedGroupID(): String =
        profile.groupID.ifBlank { profile.primaryGroupID }

    fun showPickupCoordinateError() {
        showError(L10n.markPickupOnMapShort)
    }

    fun updateAttendance(status: AttendanceStatus, dateKey: String, context: Context) {
        if (status == AttendanceStatus.Coming && store.morningPickup(profile.memberID) == null) {
            return
        }
        viewModelScope.launch {
            _pendingAttendanceStatus.value = status
            _isUpdatingAttendance.value = true
            try {
                store.setAttendance(
                    groupID = resolvedGroupID(),
                    memberID = profile.memberID,
                    name = profile.name,
                    status = status,
                    dateKey = dateKey
                )
                AttendanceUsageTracker.record(
                    context = context,
                    memberID = profile.memberID,
                    dateKey = dateKey,
                    status = status
                )
                BusTrackerAnalytics.attendanceSelected(status.analyticsValue)
                _showTripStartedAttendanceSheet.value = false
                showSuccess(L10n.choiceSaved(status.selfChoiceLabel))
            } catch (e: Exception) {
                showError(e.message ?: L10n.updateFailed)
            } finally {
                _isUpdatingAttendance.value = false
                _pendingAttendanceStatus.value = null
            }
        }
    }

    fun saveHolidayMode(endDate: Date, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSavingHolidayMode.value = true
            try {
                val groupID = resolvedGroupID()
                if (groupID.isBlank()) {
                    showError(L10n.shuttleInfoNotFound)
                    return@launch
                }
                store.setHolidayMode(groupID, profile.memberID, endDate)
                BusTrackerAnalytics.holidayModeSaved()
                showSuccess(L10n.holidayModeSaved)
                onSuccess()
            } catch (e: Exception) {
                showError(e.message ?: L10n.saveFailed)
            } finally {
                _isSavingHolidayMode.value = false
            }
        }
    }

    fun clearHolidayMode(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSavingHolidayMode.value = true
            try {
                val groupID = resolvedGroupID()
                if (groupID.isBlank()) return@launch
                store.clearHolidayMode(groupID, profile.memberID)
                BusTrackerAnalytics.holidayModeEnded()
                showSuccess(L10n.holidayModeEnded)
                onSuccess()
            } catch (e: Exception) {
                showError(e.message ?: L10n.updateFailed)
            } finally {
                _isSavingHolidayMode.value = false
            }
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            try {
                val groupID = resolvedGroupID()
                if (groupID.isBlank()) {
                    showError(L10n.shuttleInfoNotFound)
                    return@launch
                }
                store.updateMemberName(groupID, profile.memberID, newName)
                showSuccess(L10n.nameUpdated)
            } catch (e: Exception) {
                showError(e.message ?: L10n.updateFailed)
            }
        }
    }

    fun saveMorningPickup(context: Context) {
        val coordinate = _draftPickupCoordinate.value ?: run {
            showError(L10n.markPickupOnMapShort)
            return
        }

        viewModelScope.launch {
            _isSavingPickup.value = true
            try {
                val groupID = resolvedGroupID()
                store.setMorningPickup(
                    groupID = groupID,
                    memberID = profile.memberID,
                    name = profile.name,
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude
                )
                // Biniş noktası kaydedildiğinde en yakın servis için "geliyorum" kaydedilir
                val nextService = ServiceSchedule.nextTwoServices().first()
                store.setAttendance(
                    groupID = groupID,
                    memberID = profile.memberID,
                    name = profile.name,
                    status = AttendanceStatus.Coming,
                    dateKey = nextService.dateKey
                )
                AttendanceUsageTracker.record(
                    context = context,
                    memberID = profile.memberID,
                    dateKey = nextService.dateKey,
                    status = AttendanceStatus.Coming
                )
                BusTrackerAnalytics.pickupSaved()
                _showTripStartedAttendanceSheet.value = false
                _draftPickupCoordinate.value = null
                showSuccess(L10n.pickupSavedComing)
            } catch (e: Exception) {
                showError(e.message ?: L10n.saveFailed)
            } finally {
                _isSavingPickup.value = false
            }
        }
    }

}
