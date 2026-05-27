package com.mikatechnology.BusTracker.ui.passenger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PassengerHomeViewModel(
    private val profile: UserProfile
) : BaseViewModel() {

    private val store = ShuttleStore.shared

    private val _showTripStartedBanner = MutableStateFlow(false)
    val showTripStartedBanner: StateFlow<Boolean> = _showTripStartedBanner.asStateFlow()

    private val _isUpdatingAttendance = MutableStateFlow(false)
    val isUpdatingAttendance: StateFlow<Boolean> = _isUpdatingAttendance.asStateFlow()

    private val _isSavingPickup = MutableStateFlow(false)
    val isSavingPickup: StateFlow<Boolean> = _isSavingPickup.asStateFlow()

    private val _draftPickupCoordinate = MutableStateFlow<com.google.android.gms.maps.model.LatLng?>(null)
    val draftPickupCoordinate: StateFlow<com.google.android.gms.maps.model.LatLng?> = _draftPickupCoordinate.asStateFlow()

    val userProfile: UserProfile get() = profile

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
        store.startListening(groupID)
        loadSavedPickup()
    }

    fun onTripActiveChanged(wasActive: Boolean, isActive: Boolean) {
        if (isActive && !wasActive) {
            _showTripStartedBanner.value = true
            showInfo("Servis yola çıktı! Gelecek misiniz?", "Servis Başladı")
        }
    }

    fun dismissTripBanner() {
        _showTripStartedBanner.value = false
    }

    fun selectDraftCoordinate(latLng: com.google.android.gms.maps.model.LatLng) {
        _draftPickupCoordinate.value = latLng
    }

    fun clearDraftCoordinate() {
        _draftPickupCoordinate.value = null
    }

    private fun loadSavedPickup() {
        val saved = store.morningPickup(profile.memberID)
        if (_draftPickupCoordinate.value == null && saved != null) {
            _draftPickupCoordinate.value = com.google.android.gms.maps.model.LatLng(
                saved.latitude, saved.longitude
            )
        }
    }

    fun requestSignOut(onConfirm: () -> Unit) {
        showConfirm(
            title = "Çıkış Yap",
            message = "Çıkış yapmak istediğinize emin misiniz?",
            confirmTitle = "Çıkış Yap",
            destructive = true,
            onConfirm = onConfirm
        )
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            store.stopListening()
            UserSessionRepository.clear(context)
            // In a real app you would also call FirebaseAuth signOut here
        }
    }

    fun updateAttendance(status: AttendanceStatus, context: Context) {
        viewModelScope.launch {
            _isUpdatingAttendance.value = true
            try {
                store.setAttendance(
                    groupID = profile.groupID,
                    memberID = profile.memberID,
                    name = profile.name,
                    status = status
                )
            } catch (e: Exception) {
                showError(e.localizedMessage ?: "Güncellenemedi")
            } finally {
                _isUpdatingAttendance.value = false
            }
        }
    }

    fun saveMorningPickup(context: Context) {
        val coordinate = _draftPickupCoordinate.value ?: run {
            showError("Haritada biniş noktanızı işaretleyin.")
            return
        }

        viewModelScope.launch {
            _isSavingPickup.value = true
            try {
                store.setMorningPickup(
                    groupID = profile.groupID,
                    memberID = profile.memberID,
                    name = profile.name,
                    latitude = coordinate.latitude,
                    longitude = coordinate.longitude
                )
                showSuccess("Sabah biniş noktanız kaydedildi.")
            } catch (e: Exception) {
                showError(e.localizedMessage ?: "Kaydedilemedi")
            } finally {
                _isSavingPickup.value = false
            }
        }
    }

    fun copyGroupCode(context: Context, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Servis Kodu", code)
        clipboard.setPrimaryClip(clip)
        showSuccess("Servis kodu kopyalandı.")
    }
}
