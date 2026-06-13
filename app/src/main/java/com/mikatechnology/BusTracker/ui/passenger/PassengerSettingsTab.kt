package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.ui.settings.CopyServiceCode
import com.mikatechnology.BusTracker.ui.settings.SettingsCardShape
import com.mikatechnology.BusTracker.ui.settings.SettingsServiceCodeRow
import com.mikatechnology.BusTracker.ui.settings.SettingsDeleteAccountLink
import com.mikatechnology.BusTracker.ui.settings.SettingsEditableNameRow
import com.mikatechnology.BusTracker.ui.settings.SettingsNavigationRow
import com.mikatechnology.BusTracker.ui.settings.SettingsSignOutRow
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.LanguageSettingsRow
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun PassengerSettingsTab(
    profile: UserProfile,
    displayName: String,
    currentLanguage: com.mikatechnology.BusTracker.localization.AppLanguage,
    onOpenLanguagePicker: () -> Unit,
    onOpenMyServices: () -> Unit,
    onUpdateName: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (profile.groupCode.isNotBlank()) {
            SettingsServiceCodeRow(
                code = profile.groupCode,
                onClick = {
                    val copied = CopyServiceCode.copy(context, profile.groupCode)
                    CopyServiceCode.showResult(context, copied)
                }
            )
            com.mikatechnology.BusTracker.ui.settings.SettingsInviteShareRow(
                serviceCode = profile.groupCode,
                onError = { message ->
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }

        SettingsEditableNameRow(
            title = L10n.settingsYourName,
            value = displayName,
            onSave = onUpdateName
        )

        SettingsNavigationRow(
            title = L10n.myShuttles,
            value = profile.groupName,
            onClick = onOpenMyServices
        )

        LanguageSettingsRow(
            currentLanguage = currentLanguage,
            onClick = onOpenLanguagePicker
        )

        SettingsSignOutRow(onClick = onSignOut)

        SettingsDeleteAccountLink(onClick = onDeleteAccount)
    }
}
