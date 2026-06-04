package com.mikatechnology.BusTracker.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Seyrek servis modu (UI: Tatil Modu): Bitiş tarihine kadar her takvim günü ayrı kayıt.
 * Seçim yok → sürücüde gelmiyorum; o gün servise binecekse yolcu Geliyorum seçer (yalnızca o gün).
 * Örn. 3 ay açık, 2 haftada 1 gün: sadece o günlerde Geliyorum yeterli.
 */
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

    fun displayDateLabel(dateKey: String): String {
        val date = dateFromKey(dateKey) ?: return dateKey
        val formatter = SimpleDateFormat("dd.MM.yyyy", locale).apply {
            this.timeZone = timeZone
        }
        return formatter.format(date)
    }

    fun startOfTodayMillis(): Long = startOfDay(System.currentTimeMillis())

    fun tomorrowDateKey(reference: Date = Date()): String {
        val cal = calendar()
        cal.time = reference
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return dateKey(cal.time)
    }

    /** Bugünün tarih anahtarı (yolcu seçimi + sürücü listesi aynı gün belgesi). */
    fun attendancePlanningDateKey(@Suppress("UNUSED_PARAMETER") holidayModeActive: Boolean, reference: Date = Date()): String {
        return dateKey(reference)
    }

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

/** Mod açıkken: bu gün için açık Geliyorum geçerli; seçim yoksa gelmiyorum (günlük kayıt). */
fun ShuttleMember.effectiveAttendance(): AttendanceStatus {
    if (!isHolidayModeActive()) return attendance
    return when (attendance) {
        AttendanceStatus.Coming -> AttendanceStatus.Coming
        AttendanceStatus.NotComing -> AttendanceStatus.NotComing
        AttendanceStatus.Unknown -> AttendanceStatus.NotComing
    }
}
