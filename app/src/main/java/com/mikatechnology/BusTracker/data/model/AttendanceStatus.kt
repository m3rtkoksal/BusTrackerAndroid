package com.mikatechnology.BusTracker.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.ui.graphics.vector.ImageVector

enum class AttendanceStatus(val rawValue: String) {
    Coming("coming"),
    NotComing("notComing"),
    Unknown("unknown");

    val title: String
        get() = when (this) {
            Coming -> "Gelecek"
            NotComing -> "Gelmeyecek"
            Unknown -> "Belirtmedi"
        }

    val icon: ImageVector
        get() = when (this) {
            Coming -> Icons.Default.CheckCircle
            NotComing -> Icons.Default.Cancel
            Unknown -> Icons.Default.Help
        }

    companion object {
        fun fromRaw(value: String?): AttendanceStatus? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}
