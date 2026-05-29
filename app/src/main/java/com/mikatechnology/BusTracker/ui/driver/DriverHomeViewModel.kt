package com.mikatechnology.BusTracker.ui.driver

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
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

        return morningPickups.filter { pickup ->
            pickup.memberID in passengerIDs &&
                passengers.find { it.id == pickup.memberID }?.attendance != AttendanceStatus.NotComing
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

    fun handleTripControlTap(canStartTrip: Boolean) {
        if (isTripBusy) return
        if (shuttleStore.isTripActive.value) {
            stopTrip()
            return
        }
        if (!canStartTrip) {
            showError(
                "Servisi başlatmak için \"Her zaman\" konum izni zorunludur. Ayarlar'dan izin verin."
            )
            return
        }
        _showTripDurationSheet.value = true
    }

    fun confirmStartTrip(canStartTrip: Boolean) {
        if (isTripBusy) return
        if (!canStartTrip) {
            showError(
                "Servisi başlatmak için \"Her zaman\" konum izni zorunludur. Ayarlar'dan izin verin."
            )
            return
        }
        viewModelScope.launch {
            isTripBusy = true
            _showTripDurationSheet.value = false
            setLoading(true, "Servis başlatılıyor...")
            try {
                val hours = _selectedTripDurationHours.value
                shuttleStore.startTrip(profile.groupID, profile.name, hours)
                val hoursLabel = if (hours == hours.toLong().toDouble()) {
                    "${hours.toInt()} saat"
                } else {
                    "$hours saat"
                }
                showSuccess("Servis başlatıldı ($hoursLabel). Süre sonunda otomatik durur.")
            } catch (error: Exception) {
                showError(error.localizedMessage ?: "Servis başlatılamadı.")
            } finally {
                isTripBusy = false
                setLoading(false)
            }
        }
    }

    private fun stopTrip() {
        viewModelScope.launch {
            isTripBusy = true
            setLoading(true, "Servis durduruluyor...")
            try {
                shuttleStore.stopTrip(profile.groupID, profile.name)
                showSuccess("Servis durduruldu.")
            } catch (error: Exception) {
                showError(error.localizedMessage ?: "Servis durdurulamadı.")
            } finally {
                isTripBusy = false
                setLoading(false)
            }
        }
    }

    fun requestDeleteAccount(onConfirm: () -> Unit) {
        showConfirm(
            title = "Hesabı Sil",
            message = "Hesabınızı ve tüm verilerinizi kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
            confirmTitle = "Hesabı Kalıcı Olarak Sil",
            destructive = true,
            onConfirm = onConfirm
        )
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
            } finally {
                setLoading(false)
            }
        }
    }

    fun deleteAccount(context: Context) {
        viewModelScope.launch {
            setLoading(true, "Hesap siliniyor...")
            try {
                if (shuttleStore.isTripActive.value) {
                    shuttleStore.stopTrip(profile.groupID, profile.name)
                }
                shuttleStore.deleteUserData(profile)
                AuthRepository.deleteCurrentUser()
                shuttleStore.stopListening()
                UserSessionRepository.signOut(context)
                showSuccess("Hesabınız başarıyla silindi.")
            } catch (error: Exception) {
                showError("Hesap silinirken bir hata oluştu: ${error.localizedMessage ?: "Bilinmeyen hata"}")
            } finally {
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
