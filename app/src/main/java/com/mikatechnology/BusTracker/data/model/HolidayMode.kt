package com.mikatechnology.BusTracker.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object HolidayMode {
    private val locale = Locale("tr", "TR")
    private val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Istanbul")

    private fun calendar(): Calendar = Calendar.getInstance(timeZone, locale)

    fun dateKey(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", locale).apply {
            this.timeZone = timeZone
        }
        return formatter.format(date)
    }

    fun dateFromKey(key: String): Date? {
        val formatter = SimpleDateFormat("yyyy-MM-dd", locale).apply {
            this.timeZone = timeZone
        }
        return formatter.parse(key)
    }

    fun isActive(endDateKey: String, reference: Date = Date()): Boolean {
        val end = dateFromKey(endDateKey) ?: return false
        val cal = calendar()
        cal.time = reference
        val refDay = startOfDay(cal.timeInMillis)

        cal.time = end
        val endDay = startOfDay(cal.timeInMillis)
        return refDay <= endDay
    }

    fun displayDate(endDateKey: String): String? {
        val date = dateFromKey(endDateKey) ?: return null
        val formatter = SimpleDateFormat("dd.MM.yyyy", locale).apply {
            this.timeZone = timeZone
        }
        return formatter.format(date)
    }

    fun startOfTodayMillis(): Long = startOfDay(System.currentTimeMillis())

    private fun startOfDay(timeMillis: Long): Long {
        val cal = calendar()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

fun ShuttleMember.isHolidayModeActive(): Boolean {
    val key = holidayModeEndDate ?: return false
    return HolidayMode.isActive(key)
}

/** Tatil süresince: yalnızca o gün için açıkça seçilen geliyorum geçerli; belirsiz = gelmiyorum. */
fun ShuttleMember.effectiveAttendance(): AttendanceStatus {
    if (!isHolidayModeActive()) return attendance
    return when (attendance) {
        AttendanceStatus.Coming -> AttendanceStatus.Coming
        AttendanceStatus.NotComing -> AttendanceStatus.NotComing
        AttendanceStatus.Unknown -> AttendanceStatus.NotComing
    }
}
