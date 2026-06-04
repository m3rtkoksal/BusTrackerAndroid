package com.mikatechnology.BusTracker.services

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.HolidayMode
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Yolcunun açık yoklama seçimlerini (Geliyorum / Gelmiyorum) yerelde tutar;
 * seyrek kullanım önerisi için son 30/21 gün istatistiği üretir.
 */
object AttendanceUsageTracker {

    private const val PREFS_NAME = "bustracker_attendance_usage"
    private const val RETAIN_DAYS = 90

    data class WindowStats(
        val coming: Int,
        val notComing: Int
    ) {
        val explicit: Int get() = coming + notComing
    }

    fun record(
        context: Context,
        memberID: String,
        dateKey: String,
        status: AttendanceStatus
    ) {
        if (memberID.isBlank() || dateKey.isBlank()) return
        if (status == AttendanceStatus.Unknown) return

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = entriesKey(memberID)
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        val statusRaw = status.rawValue

        var replaced = false
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optString("d") == dateKey) {
                obj.put("s", statusRaw)
                replaced = true
                break
            }
        }
        if (!replaced) {
            array.put(JSONObject().put("d", dateKey).put("s", statusRaw))
        }

        val pruned = prune(array, reference = Date())
        prefs.edit().putString(key, pruned.toString()).apply()
    }

    suspend fun statsForWindowWithFirestoreSync(
        context: Context,
        groupID: String,
        memberID: String,
        windowDays: Int,
        reference: Date = Date()
    ): WindowStats {
        if (groupID.isNotBlank()) {
            syncFromFirestore(context, groupID, memberID, windowDays, reference)
        }
        return statsForWindow(context, memberID, windowDays, reference)
    }

    private suspend fun syncFromFirestore(
        context: Context,
        groupID: String,
        memberID: String,
        windowDays: Int,
        reference: Date
    ) {
        val db = FirebaseFirestore.getInstance()
        for (dateKey in SparseModeSuggestion.dateKeysForWindow(windowDays)) {
            val snapshot = runCatching {
                db.collection("groups").document(groupID)
                    .collection("attendance").document(dateKey)
                    .get()
                    .await()
            }.getOrNull() ?: continue
            if (!snapshot.exists()) continue

            @Suppress("UNCHECKED_CAST")
            val responses = snapshot.get("responses") as? Map<String, Map<String, Any>> ?: continue
            val member = responses[memberID] ?: continue
            val statusRaw = member["status"] as? String ?: continue
            val status = AttendanceStatus.fromRaw(statusRaw) ?: continue
            record(context, memberID, dateKey, status)
        }
    }

    fun statsForWindow(
        context: Context,
        memberID: String,
        windowDays: Int,
        reference: Date = Date()
    ): WindowStats {
        if (memberID.isBlank()) return WindowStats(0, 0)

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString(entriesKey(memberID), "[]") ?: "[]")
        val pruned = prune(array, reference)
        val cutoff = startOfWindow(reference, windowDays)

        var coming = 0
        var notComing = 0
        for (i in 0 until pruned.length()) {
            val obj = pruned.optJSONObject(i) ?: continue
            val dateKey = obj.optString("d")
            val day = HolidayMode.dateFromKey(dateKey)?.time ?: continue
            if (day < cutoff) continue
            when (AttendanceStatus.fromRaw(obj.optString("s"))) {
                AttendanceStatus.Coming -> coming++
                AttendanceStatus.NotComing -> notComing++
                else -> Unit
            }
        }
        return WindowStats(coming = coming, notComing = notComing)
    }

    private fun entriesKey(memberID: String) = "entries_$memberID"

    private fun prune(array: JSONArray, reference: Date): JSONArray {
        val retainCutoff = startOfWindow(reference, RETAIN_DAYS)
        val out = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val day = HolidayMode.dateFromKey(obj.optString("d"))?.time ?: continue
            if (day >= retainCutoff) {
                out.put(obj)
            }
        }
        return out
    }

    private fun startOfWindow(reference: Date, windowDays: Int): Long {
        val endDate = HolidayMode.dateFromKey(HolidayMode.dateKey(reference)) ?: reference
        val cal = Calendar.getInstance(
            TimeZone.getTimeZone("Europe/Istanbul"),
            Locale("tr", "TR")
        )
        cal.time = endDate
        cal.add(Calendar.DAY_OF_YEAR, -(windowDays - 1))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
