package com.mikatechnology.BusTracker.ui.registration

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikatechnology.BusTracker.auth.GoogleSignInHelper
import com.mikatechnology.BusTracker.base.BaseViewShell
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun LoginScreen(
    onBackToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isLoading by AuthRepository.isLoading.collectAsStateWithLifecycle()
    val uiLoading by viewModel.uiState.collectAsStateWithLifecycle()

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.signInWithGoogle(context, result.data)
    }

    BaseViewShell(viewModel = viewModel, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NeonRegistrationHero(
                icon = Icons.Default.Login,
                title = "",
                subtitle = "Google hesabınızla giriş yapın.",
                accent = NeonTheme.Secondary
            )

            NeonGlassCard(accent = NeonTheme.Secondary) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    GoogleSignInButton(
                        text = "Google ile Giriş Yap",
                        loading = isLoading || uiLoading.isLoading,
                        enabled = activity != null,
                        onClick = {
                            activity?.let {
                                googleLauncher.launch(GoogleSignInHelper.createSignInIntent(it))
                            }
                        }
                    )
                }
            }

            RegistrationBackButton(
                text = "Hesap oluşturmaya dön",
                onClick = onBackToRegister
            )
        }
    }
}
