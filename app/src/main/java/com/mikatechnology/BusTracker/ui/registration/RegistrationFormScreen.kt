package com.mikatechnology.BusTracker.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikatechnology.BusTracker.base.BaseViewShell
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.repository.AuthRepository
import com.mikatechnology.BusTracker.data.repository.ShuttleRepository
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@OptIn(ExperimentalMaterial3Api::class)
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
    val activity = context as? android.app.Activity

    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val serviceField by viewModel.serviceField.collectAsStateWithLifecycle()
    val otpCode by viewModel.otpCode.collectAsStateWithLifecycle()
    val showOTP by viewModel.showOTPVerification.collectAsStateWithLifecycle()
    val authLoading by AuthRepository.isLoading.collectAsStateWithLifecycle()
    val shuttleLoading by ShuttleRepository.shared.isLoading.collectAsStateWithLifecycle()
    val isBusy = authLoading || shuttleLoading

    val heroIcon = if (role == MemberRole.Driver) Icons.Default.DirectionsCar else Icons.Default.Person
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showOTP) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isBusy) viewModel.dismissOTP()
            },
            sheetState = sheetState,
            containerColor = NeonTheme.SurfaceContainerLow.copy(alpha = 0.98f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 16.dp)
                        .size(width = 48.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NeonTheme.SurfaceContainerHighest)
                )
            }
        ) {
            OTPVerificationSheetContent(
                formattedPhone = viewModel.formattedPhone,
                otpCode = otpCode,
                onOtpChange = viewModel::onOtpChange,
                isLoading = isBusy,
                onSubmit = { viewModel.verifyAndCreateAccount(context) },
                onResend = { activity?.let(viewModel::beginAccountCreation) },
                modifier = Modifier.navigationBarsPadding()
            )
        }
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
                    NeonPhoneFormField(
                        title = "Telefon",
                        value = phone,
                        onValueChange = viewModel::onPhoneChange
                    )
                    NeonFormField(
                        title = viewModel.serviceFieldTitle,
                        value = serviceField,
                        onValueChange = viewModel::onServiceFieldChange,
                        placeholder = viewModel.serviceFieldPrompt
                    )

                    androidx.compose.material3.Text(
                        text = viewModel.footerCaption,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = NeonTheme.OnSurfaceVariant
                    )

                    NeonPrimaryButton(
                        text = "HESABI OLUŞTUR",
                        onClick = {
                            activity?.let(viewModel::beginAccountCreation)
                        },
                        enabled = viewModel.canSubmit && activity != null,
                        loading = isBusy,
                        accent = viewModel.accent
                    )
                }
            }

            RegistrationBackButton(onClick = onBack)
        }
    }
}
