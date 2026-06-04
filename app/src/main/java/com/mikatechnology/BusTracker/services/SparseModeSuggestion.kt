package com.mikatechnology.BusTracker.services

import android.content.Context
import com.mikatechnology.BusTracker.data.model.HolidayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Seyrek servis kullanımı — sheet + bildirim kuralları. */
object SparseModeSuggestion {

    const val INTENT_TYPE = "sparse_mode_suggestion"

    private const val PREFS_NAME = "bustracker_sparse_mode_suggestion"
    private const val WINDOW_DAYS = 30
    private const val MAX_COMING_IN_MONTH = 3
    private const val MIN_EXPLICIT_IN_MONTH = 3
    private const val NOTIFICATION_COOLDOWN_DAYS = 90

    data class Prompt(val comingDays: Int)

    private fun notificationSentKey(memberID: String) = "notification_sent_$memberID"

    suspend fun evaluate(
        context: Context,
        groupID: String,
        memberID: String,
        holidayModeActive: Boolean
    ): Prompt? = withContext(Dispatchers.Default) {
        if (memberID.isBlank() || holidayModeActive) return@withContext null

        val month = AttendanceUsageTracker.statsForWindowWithFirestoreSync(
            context = context,
            groupID = groupID,
            memberID = memberID,
            windowDays = WINDOW_DAYS
        )
        if (!isEligible(month)) return@withContext null
        Prompt(comingDays = month.coming)
    }

    fun isEligible(month: AttendanceUsageTracker.WindowStats): Boolean {
        if (month.coming > MAX_COMING_IN_MONTH) return false
        if (month.explicit < MIN_EXPLICIT_IN_MONTH) return false
        if (month.notComing < month.coming && month.notComing < MIN_EXPLICIT_IN_MONTH) return false
        return true
    }

    fun shouldSendNotification(context: Context, memberID: String): Boolean {
        val sentAt = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(notificationSentKey(memberID), 0L)
        if (sentAt <= 0L) return true
        val elapsed = System.currentTimeMillis() - sentAt
        return elapsed >= TimeUnit.DAYS.toMillis(NOTIFICATION_COOLDOWN_DAYS.toLong())
    }

    fun markNotificationSent(context: Context, memberID: String) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(notificationSentKey(memberID), System.currentTimeMillis())
            .apply()
    }

    /** Tatil modu açıldığında sheet/bildirim döngüsünü sıfırlamaya gerek yok; holidayActive yeterli. */
    fun dateKeysForWindow(windowDays: Int): List<String> {
        val endKey = HolidayMode.dateKey(java.util.Date())
        val endDate = HolidayMode.dateFromKey(endKey) ?: java.util.Date()
        val cal = java.util.Calendar.getInstance(
            java.util.TimeZone.getTimeZone("Europe/Istanbul"),
            java.util.Locale("tr", "TR")
        )
        cal.time = endDate
        val keys = mutableListOf<String>()
        repeat(windowDays) {
            keys.add(0, HolidayMode.dateKey(cal.time))
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return keys
    }
}
