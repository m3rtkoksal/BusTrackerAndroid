package com.mikatechnology.BusTracker.data.model

import java.util.Date

data class MorningPickup(
    val memberID: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: Date
)
