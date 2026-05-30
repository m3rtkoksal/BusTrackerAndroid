package com.mikatechnology.BusTracker.ui.passenger

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikatechnology.BusTracker.auth.GoogleSignInHelper
import com.mikatechnology.BusTracker.base.BaseViewShell
import com.mikatechnology.BusTracker.data.model.AttendanceStatus
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.ui.services.MyServicesScreen
import com.mikatechnology.BusTracker.ui.shared.RoleNavBar
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

@Composable
fun PassengerHomeView(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    viewModel: PassengerHomeViewModel = viewModel(
        key = "passenger_home_${profile.userID}",
        factory = PassengerHomeViewModelFactory(profile)
    ),
    tabController: PassengerTabBarController = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val googleDeleteAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.deleteAccount(context, result.data)
    }

    var showMyServices by remember { mutableStateOf(false) }
    val selectedTab by tabController.selectedTab.collectAsState()
    val isTripActive by ShuttleStore.shared.isTripActive.collectAsState()
    val driverLocation by ShuttleStore.shared.driverLocation.collectAsState()
    val driverRoute by ShuttleStore.shared.driverRoute.collectAsState()
    val morningPickups by ShuttleStore.shared.morningPickups.collectAsState()
    val members by ShuttleStore.shared.members.collectAsState()

    val showTripAttendanceSheet by viewModel.showTripStartedAttendanceSheet.collectAsState()
    val pendingAttendance by viewModel.pendingAttendanceStatus.collectAsState()
    val isUpdatingAttendance by viewModel.isUpdatingAttendance.collectAsState()
    val isSavingPickup by viewModel.isSavingPickup.collectAsState()
    val draftCoordinate by viewModel.draftPickupCoordinate.collectAsState()

    val myMember = members.firstOrNull { it.id == profile.memberID }
    val myAttendance = myMember?.attendance ?: AttendanceStatus.Unknown

    val savedPickup = remember(morningPickups, profile.memberID) {
        ShuttleStore.shared.morningPickup(profile.memberID)
    }

    var wasTripActive by remember { mutableStateOf(isTripActive) }

    LaunchedEffect(profile.groupID) {
        viewModel.onAppear(profile.groupID)
    }

    LaunchedEffect(isTripActive, myAttendance) {
        if (isTripActive && !wasTripActive) {
            viewModel.onTripActiveChanged(false, true, myAttendance)
        } else if (!isTripActive && wasTripActive) {
            viewModel.onTripActiveChanged(true, false, myAttendance)
        }
        wasTripActive = isTripActive
        viewModel.presentTripAttendanceSheetIfNeeded(isTripActive, myAttendance)
    }

    BaseViewShell(viewModel = viewModel, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedTab != PassengerHomeTab.Map) {
                PassengerTopBar(
                    isTripActive = isTripActive,
                    onMenuClick = { showMyServices = true }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    PassengerHomeTab.Service -> {
                        PassengerServiceTab(
                            profile = profile,
                            isTripActive = isTripActive,
                            myAttendance = myAttendance,
                            savedMorningPickup = savedPickup,
                            isUpdatingAttendance = isUpdatingAttendance,
                            onAttendanceSelected = { status ->
                                viewModel.updateAttendance(status, context)
                            },
                            onOpenMap = { tabController.select(PassengerHomeTab.Map) }
                        )
                    }

                    PassengerHomeTab.Map -> {
                        // NOTE: Full interactive passenger map (tap to pick location + custom neon markers)
                        // is implemented in PassengerMapTabView. Using simplified version for now.
                        PassengerMapTabView(
                            groupName = profile.groupName,
                            driverLocation = driverLocation,
                            driverRoute = driverRoute,
                            draftCoordinate = draftCoordinate,
                            savedPickup = savedPickup,
                            isTripActive = isTripActive,
                            isSaving = isSavingPickup,
                            onMapClick = { latLng ->
                                viewModel.selectDraftCoordinate(latLng)
                            },
                            onSavePickup = {
                                viewModel.saveMorningPickup(context)
                            }
                        )
                    }

                    PassengerHomeTab.Settings -> {
                        PassengerSettingsTab(
                            profile = profile,
                            onCopyCode = {
                                viewModel.copyGroupCode(context, profile.groupCode)
                            },
                            onSignOut = {
                                viewModel.requestSignOut {
                                    viewModel.signOut(context)
                                }
                            },
                            onDeleteAccount = {
                                viewModel.requestDeleteAccount {
                                    activity?.let { act ->
                                        googleDeleteAccountLauncher.launch(
                                            GoogleSignInHelper.createSignInIntent(act)
                                        )
                                    } ?: viewModel.deleteAccount(context, null)
                                }
                            }
                        )
                    }
                }
            }

            PassengerTabBar(
                selectedTab = selectedTab,
                onTabSelected = tabController::select
            )
        }

            if (showMyServices) {
                MyServicesScreen(
                    onBack = { showMyServices = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                )
            }

            AnimatedVisibility(
                visible = showTripAttendanceSheet,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                            .clickable { viewModel.dismissTripAttendanceSheet() }
                    )
                    TripStartedAttendanceSheet(
                        driverName = driverLocation?.driverName ?: "Sürücü",
                        isLoading = isUpdatingAttendance,
                        pendingStatus = pendingAttendance,
                        onSelectComing = { viewModel.updateAttendance(AttendanceStatus.Coming, context) },
                        onSelectNotComing = { viewModel.updateAttendance(AttendanceStatus.NotComing, context) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun PassengerTopBar(
    isTripActive: Boolean,
    onMenuClick: () -> Unit
) {
    RoleNavBar(onMenuClick = onMenuClick) {
        if (isTripActive) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonTheme.Secondary)
                        .shadow(4.dp, spotColor = NeonTheme.Secondary.copy(alpha = 0.8f))
                )
                Text(
                    text = "CANLI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NeonTheme.Secondary
                )
            }
        }
    }
}
