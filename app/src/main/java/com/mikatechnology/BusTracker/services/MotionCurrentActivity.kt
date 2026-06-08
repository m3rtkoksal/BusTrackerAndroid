package com.mikatechnology.BusTracker.services

enum class MotionCurrentActivity(val rawValue: String) {
    InVehicle("in_vehicle"),
    Walking("walking"),
    Stationary("stationary"),
    Unknown("unknown")
}
