package com.mikatechnology.BusTracker.data.model

import com.mikatechnology.BusTracker.localization.AppLanguage
import com.mikatechnology.BusTracker.localization.LanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DriverSubscriptionInfo(
    val startDate: Date? = null,
    val endDate: Date? = null
) {
    val isActive: Boolean
        get() {
            val end = endDate ?: return false
            val todayKey = HolidayMode.dateKey(Date())
            val endKey = HolidayMode.dateKey(end)
            return endKey >= todayKey
        }

    val daysRemaining: Int?
        get() {
            val end = endDate ?: return null
            if (!isActive) return null
            val today = HolidayMode.dateFromKey(HolidayMode.dateKey(Date())) ?: return null
            val endDay = HolidayMode.dateFromKey(HolidayMode.dateKey(end)) ?: return null
            val diffMs = endDay.time - today.time
            return (diffMs / (24 * 60 * 60 * 1000)).toInt()
        }

    val isExpiringSoon: Boolean
        get() {
            val days = daysRemaining ?: return false
            return days in 0..EXPIRING_SOON_DAYS
        }

    companion object {
        const val EXPIRING_SOON_DAYS = 7
        val Empty = DriverSubscriptionInfo()
    }
}

object DriverSubscriptionDisplay {
    fun formatDate(date: Date?): String {
        if (date == null) return "—"
        val locale = when (LanguageManager.language.value) {
            AppLanguage.Turkish -> Locale("tr", "TR")
            AppLanguage.English -> Locale.ENGLISH
        }
        val formatter = SimpleDateFormat("d MMMM yyyy", locale)
        return formatter.format(date)
    }
}
