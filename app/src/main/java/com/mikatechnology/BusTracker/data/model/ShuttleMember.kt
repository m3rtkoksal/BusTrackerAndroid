package com.mikatechnology.BusTracker.data.model

data class ShuttleMember(
    val id: String,
    val name: String,
    val role: MemberRole,
    val attendance: AttendanceStatus = AttendanceStatus.Unknown,
    val boardedAt: java.util.Date? = null,
    /** `yyyy-MM-dd` — bitiş günü dahil tatil. */
    val holidayModeEndDate: String? = null
) {
    val isBoardedToday: Boolean
        get() = boardedAt != null
}
