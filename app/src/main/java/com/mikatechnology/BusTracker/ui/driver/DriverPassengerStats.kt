package com.mikatechnology.BusTracker.ui.driver

data class DriverPassengerStats(
    val total: Int = 15,           // Maksimum kapasite (sabit 15)
    val coming: Int,
    val notComing: Int,
    val unknown: Int
) {
    /** Gelmiyorum dışındaki tüm yolcular (geliyorum + belirtmedi). */
    val capacityOccupied: Int
        get() = coming + unknown

    val comingProgress: Double
        get() = if (total > 0) capacityOccupied.toDouble() / total else 0.0
}
