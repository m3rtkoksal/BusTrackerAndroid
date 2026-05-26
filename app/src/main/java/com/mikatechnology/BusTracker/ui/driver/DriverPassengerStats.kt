package com.mikatechnology.BusTracker.ui.driver

data class DriverPassengerStats(
    val total: Int,
    val coming: Int,
    val notComing: Int,
    val unknown: Int
) {
    val comingProgress: Double
        get() = if (total > 0) coming.toDouble() / total else 0.0
}
