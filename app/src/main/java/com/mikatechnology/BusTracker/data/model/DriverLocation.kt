package com.mikatechnology.BusTracker.data.model

import java.util.Date

data class DriverLocation(
    val latitude: Double,
    val longitude: Double,
    val updatedAt: Date,
    val isActive: Boolean,
    val driverName: String
)
