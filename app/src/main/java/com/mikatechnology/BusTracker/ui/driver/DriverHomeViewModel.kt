package com.mikatechnology.BusTracker.ui.driver

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.data.repository.UserSessionRepository
import com.mikatechnology.BusTracker.services.LocationTracker
import kotlinx.coroutines.launch

class DriverHomeViewModel(
    private val profile: UserProfile
) : BaseViewModel() {

    private val shuttleStore = ShuttleStore.shared
    private var isTripBusy = false

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
    }

    fun passengerStats(members: List<ShuttleMember>): DriverPassengerStats {
        val passengers = members.filter { it.role == MemberRole.Passenger }
        return DriverPassengerStats(
            total = passengers.size,
            coming = passengers.count { it.attendance == AttendanceStatus.Coming },
            notComing = passengers.count { it.attendance == AttendanceStatus.NotComing },
            unknown = passengers.count { it.attendance == AttendanceStatus.Unknown }
        )
    }

    fun passengerMorningPickups(
        passengers: List<ShuttleMember>,
        morningPickups: List<MorningPickup>
    ): List<MorningPickup> {
        val passengerIDs = passengers.map { it.id }.toSet()
        return morningPickups.filter { it.memberID in passengerIDs }
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

    fun toggleTrip(context: Context) {
        if (isTripBusy) return
        viewModelScope.launch {
            isTripBusy = true
            setLoading(true, if (shuttleStore.isTripActive.value) "Servis durduruluyor..." else "Servis başlatılıyor...")
            try {
                if (shuttleStore.isTripActive.value) {
                    shuttleStore.stopTrip(profile.groupID, profile.name)
                } else {
                    shuttleStore.startTrip(profile.groupID, profile.name)
                    showSuccess("Servis başlatıldı. Yolcular bilgilendirildi.")
                }
            } catch (error: Exception) {
                showError(error.localizedMessage ?: "Servis işlemi başarısız.")
            } finally {
                isTripBusy = false
                setLoading(false)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            setLoading(true, "Çıkış yapılıyor...")
            try {
                if (shuttleStore.isTripActive.value) {
                    shuttleStore.stopTrip(profile.groupID, profile.name)
                }
                shuttleStore.stopListening()
                UserSessionRepository.signOut(context)
            } catch (error: Exception) {
                showError(error.localizedMessage ?: "Çıkış yapılamadı.")
                setLoading(false)
            }
        }
    }

    fun copyGroupCode(context: Context, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Servis kodu", code))
        showSuccess("Servis kodu kopyalandı.")
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
