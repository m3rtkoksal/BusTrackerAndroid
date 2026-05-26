package com.mikatechnology.BusTracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.base.BaseViewModel
import com.mikatechnology.BusTracker.base.BaseViewShell
import com.mikatechnology.BusTracker.base.NavigationBarStyle
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

private class LoggedInPlaceholderViewModel(
    title: String
) : BaseViewModel() {
    init {
        configureScreen(
            title = title,
            navigationBarStyle = NavigationBarStyle.NeonPassenger,
            hidesNavigationBar = true,
            embedsInNavigationStack = false
        )
    }
}

@Composable
fun LoggedInPlaceholderScreen(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val viewModel = remember(profile.userID) {
        LoggedInPlaceholderViewModel(title = profile.groupName)
    }
    BaseViewShell(viewModel = viewModel, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hoş geldin, ${profile.name}!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTheme.OnSurface
            )
            Text(
                text = "${profile.role.title} • ${profile.groupName}",
                color = NeonTheme.Secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Ana ekranlar bir sonraki adımda eklenecek.",
                color = NeonTheme.OnSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
