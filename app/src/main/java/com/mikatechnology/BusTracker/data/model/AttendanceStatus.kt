package com.mikatechnology.BusTracker.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.ui.graphics.vector.ImageVector
import com.mikatechnology.BusTracker.localization.L10n

enum class AttendanceStatus(val rawValue: String) {
    Coming("coming"),
    NotComing("notComing"),
    Unknown("unknown");

    val title: String
        get() = when (this) {
            Coming -> L10n.attendanceComing
            NotComing -> L10n.attendanceNotComing
            Unknown -> L10n.attendanceUnknown
        }

    val icon: ImageVector
        get() = when (this) {
            Coming -> Icons.Default.CheckCircle
            NotComing -> Icons.Default.Cancel
            Unknown -> Icons.Default.Help
        }

    val mapTabLabel: String
        get() = when (this) {
            Coming -> L10n.attendanceComingSelf
            NotComing -> L10n.attendanceNotComingSelf
            Unknown -> L10n.attendanceUncertain
        }

    fun mapTabLabel(isBoarded: Boolean): String =
        if (isBoarded) L10n.attendanceBoardedSelf else mapTabLabel

    companion object {
        fun fromRaw(value: String?): AttendanceStatus? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}
