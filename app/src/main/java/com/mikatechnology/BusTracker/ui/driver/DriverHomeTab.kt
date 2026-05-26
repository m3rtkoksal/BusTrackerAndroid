package com.mikatechnology.BusTracker.ui.driver

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class DriverHomeTab {
    Passengers,
    Map,
    Settings;

    val title: String
        get() = when (this) {
            Passengers -> "Yolcular"
            Map -> "Harita"
            Settings -> "Ayarlar"
        }

    val icon: ImageVector
        get() = when (this) {
            Passengers -> Icons.Default.Groups
            Map -> Icons.Default.Map
            Settings -> Icons.Default.Tune
        }
}
