package com.mikatechnology.BusTracker.data.model

data class ShuttleMember(
    val id: String,
    val name: String,
    val role: MemberRole,
    val attendance: AttendanceStatus = AttendanceStatus.Unknown
)
