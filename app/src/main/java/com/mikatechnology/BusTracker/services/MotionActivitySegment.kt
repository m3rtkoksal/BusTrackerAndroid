package com.mikatechnology.BusTracker.services

import java.util.Date

data class MotionActivitySegment(
    val startedAt: Date,
    val endedAt: Date,
    val isAutomotive: Boolean
)
