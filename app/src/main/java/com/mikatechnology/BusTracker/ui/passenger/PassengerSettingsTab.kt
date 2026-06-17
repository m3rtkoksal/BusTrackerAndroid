package com.mikatechnology.BusTracker.ui.passenger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.driver.DriverSubscriptionScreen
import com.mikatechnology.BusTracker.ui.driver.DriverSubscriptionViewModel
import com.mikatechnology.BusTracker.ui.settings.CopyServiceCode
import com.mikatechnology.BusTracker.ui.settings.LanguageSettingsRow
import com.mikatechnology.BusTracker.ui.settings.SettingsDeleteAccountLink
import com.mikatechnology.BusTracker.ui.settings.SettingsEditableNameRow
import com.mikatechnology.BusTracker.ui.settings.SettingsInviteShareRow
import com.mikatechnology.BusTracker.ui.settings.SettingsNavigationRow
import com.mikatechnology.BusTracker.ui.settings.SettingsServiceCodeRow
import com.mikatechnology.BusTracker.ui.settings.SettingsSignOutRow

@Composable
fun PassengerSettingsTab(
    profile: UserProfile,
    displayName: String,
    currentLanguage: com.mikatechnology.BusTracker.localization.AppLanguage,
    subscriptionViewModel: DriverSubscriptionViewModel,
    onOpenLanguagePicker: () -> Unit,
    onOpenMyServices: () -> Unit,
    onUpdateName: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "settings_root",
        modifier = modifier.fillMaxSize()
    ) {
        composable("settings_root") {
            Column(
                modifier = Modifier
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
                    SettingsInviteShareRow(
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

                SettingsNavigationRow(
                    title = L10n.subscription,
                    value = subscriptionViewModel.statusSubtitle,
                    onClick = { navController.navigate("subscription") }
                )

                LanguageSettingsRow(
                    currentLanguage = currentLanguage,
                    onClick = onOpenLanguagePicker
                )

                SettingsSignOutRow(onClick = onSignOut)

                SettingsDeleteAccountLink(onClick = onDeleteAccount)
            }
        }

        composable("subscription") {
            DriverSubscriptionScreen(
                groupID = profile.primaryGroupID,
                viewModel = subscriptionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
