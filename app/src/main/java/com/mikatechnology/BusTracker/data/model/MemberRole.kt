package com.mikatechnology.BusTracker.data.model

enum class MemberRole(val rawValue: String) {
    Driver("driver"),
    Passenger("passenger");

    val title: String
        get() = when (this) {
            Driver -> "Sürücü"
            Passenger -> "Yolcu"
        }

    companion object {
        fun fromRoute(value: String): MemberRole = when (value.lowercase()) {
            "driver" -> Driver
            "passenger" -> Passenger
            else -> Passenger
        }
    }
}
