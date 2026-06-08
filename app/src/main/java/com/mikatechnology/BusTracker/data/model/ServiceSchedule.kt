package com.mikatechnology.BusTracker.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Servis seansı: Sabah veya Akşam
 */
enum class ServiceSession(val suffix: String) {
    Morning("am"),
    Evening("pm");

    val displayName: String
        get() = when (this) {
            Morning -> "Sabah"
            Evening -> "Akşam"
        }

    val icon: String
        get() = when (this) {
            Morning -> "☀️"
            Evening -> "🌙"
        }
}

/**
 * Yaklaşan bir servis: tarih + seans
 */
data class UpcomingService(
    val date: Date,
    val session: ServiceSession
) {
    private val locale = Locale("tr", "TR")
    private val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Istanbul")

    val dateKey: String
        get() {
            val base = HolidayMode.dateKey(date)
            return "$base-${session.suffix}"
        }

    val dayName: String
        get() {
            val formatter = SimpleDateFormat("EEEE", locale).apply {
                this.timeZone = this@UpcomingService.timeZone
            }
            return formatter.format(date).replaceFirstChar { it.uppercase() }
        }

    val displayDate: String
        get() {
            val formatter = SimpleDateFormat("d MMMM yyyy", locale).apply {
                this.timeZone = this@UpcomingService.timeZone
            }
            return formatter.format(date)
        }

    val fullDisplayName: String
        get() = "$dayName ${session.displayName}"

    fun relativeDisplayName(reference: Date = Date()): String {
        val cal = Calendar.getInstance(timeZone, locale)

        cal.time = reference
        val refDay = startOfDay(cal)

        cal.time = date
        val serviceDay = startOfDay(cal)

        val dayDiff = ((serviceDay - refDay) / (24 * 60 * 60 * 1000)).toInt()

        val dayPart = when (dayDiff) {
            0 -> "Bugün"
            1 -> "Yarın"
            else -> dayName
        }

        return "$dayPart ${session.displayName}"
    }

    private fun startOfDay(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

/**
 * Servis zamanlama hesaplamaları
 */
object ServiceSchedule {
    private val locale = Locale("tr", "TR")
    private val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Istanbul")

    private fun calendar(): Calendar = Calendar.getInstance(timeZone, locale)

    // MARK: - Sabit Saatler

    /** Sabah servisi bitiş saati (bu saatten sonra sabah servisi geçmiş sayılır) */
    private const val MORNING_END_HOUR = 10

    /** Akşam servisi başlangıç saati */
    private const val EVENING_START_HOUR = 16

    /** Akşam servisi bitiş saati (bu saatten sonra akşam servisi geçmiş sayılır) */
    private const val EVENING_END_HOUR = 20

    // MARK: - Yolcu: Sonraki 2 Servis

    /**
     * Yolcunun göreceği sonraki 2 servisi hesaplar
     * @param reference Şu anki zaman (test için override edilebilir)
     * @return Sonraki 2 servis (her zaman 2 eleman döner)
     */
    fun nextTwoServices(reference: Date = Date()): List<UpcomingService> {
        val cal = calendar()
        cal.time = reference
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val weekday = cal.get(Calendar.DAY_OF_WEEK)

        // Cumartesi (7) veya Pazar (1) - hafta sonu
        val isWeekend = weekday == Calendar.SUNDAY || weekday == Calendar.SATURDAY

        if (isWeekend) {
            // Hafta sonu: Pazartesi sabah + Pazartesi akşam
            val monday = nextWeekday(reference)
            return listOf(
                UpcomingService(monday, ServiceSession.Morning),
                UpcomingService(monday, ServiceSession.Evening)
            )
        }

        val today = startOfDay(reference)

        return when {
            hour < MORNING_END_HOUR -> {
                // 00:00 - 09:59: Bugün sabah + Bugün akşam
                listOf(
                    UpcomingService(today, ServiceSession.Morning),
                    UpcomingService(today, ServiceSession.Evening)
                )
            }
            hour < EVENING_END_HOUR -> {
                // 10:00 - 19:59: Bugün akşam + Sonraki iş günü sabah
                val nextWorkday = nextWeekday(reference, afterToday = true)
                listOf(
                    UpcomingService(today, ServiceSession.Evening),
                    UpcomingService(nextWorkday, ServiceSession.Morning)
                )
            }
            else -> {
                // 20:00 - 23:59: Sonraki iş günü sabah + akşam
                val nextWorkday = nextWeekday(reference, afterToday = true)
                listOf(
                    UpcomingService(nextWorkday, ServiceSession.Morning),
                    UpcomingService(nextWorkday, ServiceSession.Evening)
                )
            }
        }
    }

    // MARK: - Sürücü: Hangi Servis Modunda?

    /**
     * Sürücünün göreceği servis seansını hesaplar
     * @param reference Şu anki zaman
     * @return UpcomingService
     */
    fun currentDriverSession(reference: Date = Date()): UpcomingService {
        val cal = calendar()
        cal.time = reference
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val weekday = cal.get(Calendar.DAY_OF_WEEK)

        // Cumartesi (7) veya Pazar (1) - hafta sonu
        val isWeekend = weekday == Calendar.SUNDAY || weekday == Calendar.SATURDAY

        if (isWeekend) {
            // Hafta sonu: Pazartesi sabah
            val monday = nextWeekday(reference)
            return UpcomingService(monday, ServiceSession.Morning)
        }

        val today = startOfDay(reference)

        return when {
            hour < MORNING_END_HOUR -> {
                // 00:00 - 09:59: Sabah servisi
                UpcomingService(today, ServiceSession.Morning)
            }
            hour < EVENING_END_HOUR -> {
                // 10:00 - 19:59: Akşam servisi
                UpcomingService(today, ServiceSession.Evening)
            }
            else -> {
                // 20:00 - 23:59: Sonraki iş günü sabah
                val nextWorkday = nextWeekday(reference, afterToday = true)
                UpcomingService(nextWorkday, ServiceSession.Morning)
            }
        }
    }

    // MARK: - Yardımcı Fonksiyonlar

    /**
     * Sonraki hafta içi günü bulur
     * @param reference Başlangıç tarihi
     * @param afterToday true ise bugünü atlar (yarından başlar)
     * @return Sonraki hafta içi gün
     */
    private fun nextWeekday(reference: Date, afterToday: Boolean = false): Date {
        val cal = calendar()
        cal.time = reference
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (afterToday) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Hafta sonu ise Pazartesi'ye atla
        var weekday = cal.get(Calendar.DAY_OF_WEEK)
        while (weekday == Calendar.SUNDAY || weekday == Calendar.SATURDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            weekday = cal.get(Calendar.DAY_OF_WEEK)
        }

        return cal.time
    }

    // MARK: - DateKey Yardımcıları

    /**
     * DateKey'den tarih ve seans çıkarır
     * @param dateKey "2026-06-09-am" formatında key
     * @return Pair(date, session) veya null
     */
    fun parse(dateKey: String): Pair<Date, ServiceSession>? {
        val parts = dateKey.split("-")
        if (parts.size != 4) return null

        val session = when (parts[3]) {
            "am" -> ServiceSession.Morning
            "pm" -> ServiceSession.Evening
            else -> return null
        }

        val baseDateKey = parts.subList(0, 3).joinToString("-")
        val date = HolidayMode.dateFromKey(baseDateKey) ?: return null

        return Pair(date, session)
    }

    /**
     * Bugünün sabah ve akşam dateKey'lerini döndürür
     */
    fun todayDateKeys(reference: Date = Date()): Pair<String, String> {
        val today = startOfDay(reference)
        val base = HolidayMode.dateKey(today)
        return Pair("$base-am", "$base-pm")
    }

    private fun startOfDay(reference: Date): Date {
        val cal = calendar()
        cal.time = reference
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }
}
