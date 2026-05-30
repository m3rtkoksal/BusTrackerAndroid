package com.mikatechnology.BusTracker.ui.passenger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.MorningPickup
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.AuthRepository
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

    private val _showTripStartedAttendanceSheet = MutableStateFlow(false)
    val showTripStartedAttendanceSheet: StateFlow<Boolean> = _showTripStartedAttendanceSheet.asStateFlow()

    private val _pendingAttendanceStatus = MutableStateFlow<AttendanceStatus?>(null)
    val pendingAttendanceStatus: StateFlow<AttendanceStatus?> = _pendingAttendanceStatus.asStateFlow()

    private var didPromptAttendanceThisTrip = false

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
        val resolvedGroupID = groupID.ifBlank { profile.primaryGroupID }
        if (resolvedGroupID.isBlank()) {
            showError("Servis bilgisi bulunamadı. Çıkış yapıp servise yeniden katılın.")
            return
        }
        store.startListening(resolvedGroupID)
        loadSavedPickup()
    }

    fun onTripActiveChanged(wasActive: Boolean, isActive: Boolean, attendance: AttendanceStatus) {
        if (!isActive) {
            didPromptAttendanceThisTrip = false
            _showTripStartedAttendanceSheet.value = false
            return
        }
        if (isActive && !wasActive) {
            presentTripAttendanceSheetIfNeeded(isTripActive = true, attendance = attendance)
        }
    }

    fun presentTripAttendanceSheetIfNeeded(isTripActive: Boolean, attendance: AttendanceStatus) {
        if (!isTripActive || attendance != AttendanceStatus.Unknown || didPromptAttendanceThisTrip) return
        didPromptAttendanceThisTrip = true
        _showTripStartedAttendanceSheet.value = true
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

    fun requestDeleteAccount(onConfirm: () -> Unit) {
        showConfirm(
            title = "Hesabı Sil",
            message = "Hesabınızı ve tüm verilerinizi (profil, biniş noktaları, katılım kayıtları vb.) kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
            confirmTitle = "Hesabı Kalıcı Olarak Sil",
            destructive = true,
            onConfirm = onConfirm
        )
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            setLoading(true, "Çıkış yapılıyor...")
            try {
                store.stopListening()
                UserSessionRepository.signOut(context)
            } catch (e: Exception) {
                showError(e.localizedMessage ?: "Çıkış yapılamadı.")
            } finally {
                setLoading(false)
            }
        }
    }

    fun deleteAccount(context: Context, googleReauthData: Intent?) {
        viewModelScope.launch {
            if (googleReauthData == null) {
                showError("Hesap silmek için Google doğrulaması gerekli.")
                return@launch
            }

            setLoading(true, "Hesap siliniyor...")
            var profileDeleted = false
            var authRemoved = false
            var shouldSignOut = false
            try {
                if (!AuthRepository.reauthenticateWithGoogle(googleReauthData)) {
                    showError("Google doğrulaması tamamlanamadı.")
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
                    showSuccess("Hesabınız başarıyla silindi.")
                } else if (profileDeleted || authRemoved) {
                    showSuccess("Hesabınız başarıyla silindi.")
                } else {
                    showError("Hesap silinirken bir hata oluştu. Oturumunuz kapatıldı; tekrar deneyebilirsiniz.")
                }
            }
        }
    }

    private fun resolvedGroupID(): String =
        profile.groupID.ifBlank { profile.primaryGroupID }

    fun updateAttendance(status: AttendanceStatus, context: Context) {
        viewModelScope.launch {
            _pendingAttendanceStatus.value = status
            _isUpdatingAttendance.value = true
            try {
                store.setAttendance(
                    groupID = resolvedGroupID(),
                    memberID = profile.memberID,
                    name = profile.name,
                    status = status
                )
                _showTripStartedAttendanceSheet.value = false
                showSuccess("Seçiminiz kaydedildi: ${status.title}")
            } catch (e: Exception) {
                showError(e.localizedMessage ?: "Güncellenemedi")
            } finally {
                _isUpdatingAttendance.value = false
                _pendingAttendanceStatus.value = null
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
                    groupID = resolvedGroupID(),
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
