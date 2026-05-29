package com.mikatechnology.BusTracker.ui.registration

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikatechnology.BusTracker.auth.GoogleSignInHelper
import com.mikatechnology.BusTracker.base.BaseViewShell
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository

@Composable
fun RegistrationFormScreen(
    role: MemberRole,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistrationFormViewModel = viewModel(
        key = "registration_${role.name}",
        factory = RegistrationFormViewModelFactory(role)
    )
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val name by viewModel.name.collectAsStateWithLifecycle()
    val serviceField by viewModel.serviceField.collectAsStateWithLifecycle()
    val authLoading by AuthRepository.isLoading.collectAsStateWithLifecycle()
    val shuttleLoading by ShuttleRepository.shared.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serviceFieldError by viewModel.serviceFieldError.collectAsStateWithLifecycle()
    val isBusy = authLoading || shuttleLoading || uiState.isLoading
    val canTapGoogleSignIn = activity != null && !isBusy

    val heroIcon = if (role == MemberRole.Driver) Icons.Default.DirectionsCar else Icons.Default.Person

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Google Sign-In often returns RESULT_CANCELED even on success; parse the Intent instead.
        viewModel.createAccountWithGoogle(context, result.data)
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
                icon = heroIcon,
                title = viewModel.heroTitle,
                subtitle = viewModel.heroSubtitle,
                accent = viewModel.accent
            )

            NeonGlassCard(accent = viewModel.accent) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    NeonFormField(
                        title = "Adınız",
                        value = name,
                        onValueChange = viewModel::onNameChange,
                        placeholder = viewModel.namePrompt
                    )
                    NeonFormField(
                        title = viewModel.serviceFieldTitle,
                        value = serviceField,
                        onValueChange = viewModel::onServiceFieldChange,
                        placeholder = viewModel.serviceFieldPrompt,
                        errorText = serviceFieldError
                    )

                    androidx.compose.material3.Text(
                        text = viewModel.footerCaption,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = com.mikatechnology.BusTracker.ui.theme.NeonTheme.OnSurfaceVariant
                    )

                    androidx.compose.material3.Text(
                        text = "Kayıt için Google hesabınız kullanılır; telefon numarası istenmez.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = com.mikatechnology.BusTracker.ui.theme.NeonTheme.OnSurfaceVariant
                    )

                    GoogleSignInButton(
                        text = "Google ile Kayıt Ol",
                        loading = isBusy,
                        enabled = canTapGoogleSignIn,
                        onClick = {
                            if (viewModel.validateBeforeGoogleSignIn()) {
                                activity?.let {
                                    googleLauncher.launch(
                                        GoogleSignInHelper.createSignInIntent(it)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            RegistrationBackButton(onClick = onBack)
        }
    }
}
