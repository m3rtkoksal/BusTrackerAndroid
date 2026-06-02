package com.mikatechnology.BusTracker.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikatechnology.BusTracker.base.BaseViewShell
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun RoleSelectionScreen(
    onLoginTapped: () -> Unit,
    onSelectDriver: () -> Unit,
    onSelectPassenger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = remember(onLoginTapped) {
        RoleSelectionViewModel(onLoginTapped = onLoginTapped)
    }

    BaseViewShell(viewModel = viewModel, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NeonRoleCard(
                title = L10n.iAmDriver,
                subtitle = L10n.iAmDriverSubtitle,
                icon = DriverHeroIcon,
                accent = NeonTheme.Primary,
                onClick = onSelectDriver
            )

            NeonRoleCard(
                title = L10n.iAmPassenger,
                subtitle = L10n.iAmPassengerSubtitle,
                icon = PassengerHeroIcon,
                accent = NeonTheme.Secondary,
                onClick = onSelectPassenger
            )

            TextButton(onClick = viewModel::loginTapped) {
                Text(
                    text = "${L10n.alreadyHaveAccount} ",
                    color = NeonTheme.OnSurfaceVariant
                )
                Text(
                    text = L10n.signInAction,
                    color = NeonTheme.Secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
