package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.ui.graphics.vector.ImageVector

enum class PassengerHomeTab {
    Service,
    Map,
    Settings;

    val title: String
        get() = when (this) {
            Service -> "Servis"
            Map -> "Harita"
            Settings -> "Ayarlar"
        }

    val icon: ImageVector
        get() = when (this) {
            Service -> Icons.Default.DirectionsBus
            Map -> Icons.Default.Map
            Settings -> Icons.Default.Settings
        }
}
