package com.mikatechnology.BusTracker.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikatechnology.BusTracker.data.smler.SmlerDeepLinkService
import com.mikatechnology.BusTracker.data.smler.SmlerShareOutcome
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsInviteShareRow(
    serviceCode: String,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }
    var isLoadingLink by remember { mutableStateOf(false) }

    LaunchedEffect(serviceCode) {
        isLoadingLink = true
        try {
            SmlerDeepLinkService.ensureInviteLink(serviceCode)
        } finally {
            isLoadingLink = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NeonTheme.SurfaceContainer)
            .border(1.dp, NeonTheme.Outline.copy(alpha = 0.3f), SettingsCardShape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = L10n.inviteLinkTitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = NeonTheme.OnSurfaceVariant
        )

        if (isSharing || isLoadingLink) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = NeonTheme.Secondary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                modifier = Modifier
                    .background(NeonTheme.Secondary.copy(alpha = 0.12f))
                    .border(1.dp, NeonTheme.Secondary.copy(alpha = 0.4f), SettingsCardShape)
                    .clickable(enabled = !isSharing) {
                        isSharing = true
                        scope.launch {
                            try {
                                when (val outcome = SmlerDeepLinkService.prepareShare(serviceCode)) {
                                    is SmlerShareOutcome.Success -> {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, outcome.message)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    }
                                    is SmlerShareOutcome.Failure -> onError(outcome.error)
                                }
                            } finally {
                                isSharing = false
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = NeonTheme.Secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = L10n.share.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = NeonTheme.Secondary
                )
            }
        }
    }
}
