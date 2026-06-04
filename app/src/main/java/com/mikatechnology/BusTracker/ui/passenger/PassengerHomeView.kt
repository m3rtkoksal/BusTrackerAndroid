package com.mikatechnology.BusTracker.ui.passenger

import android.Manifest
import android.app.Activity
import android.os.Build
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.mikatechnology.BusTracker.data.model.HolidayMode
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.model.effectiveAttendance
import com.mikatechnology.BusTracker.data.model.isHolidayModeActive
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.services.MotionActivityRole
import com.mikatechnology.BusTracker.services.MotionActivityService
import com.mikatechnology.BusTracker.services.PushNotificationRouter
import com.mikatechnology.BusTracker.ui.services.MyServicesScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikatechnology.BusTracker.localization.LanguageManager
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.LanguagePickerOverlay
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val googleDeleteAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.deleteAccount(context, result.data)
    }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        MotionActivityService.refreshAuthorization(context)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            profile.primaryGroupID.isNotBlank() &&
            profile.memberID.isNotBlank()
        ) {
            MotionActivityService.updateMonitoring(
                context = context,
                isEnabled = true,
                role = MotionActivityRole.Passenger,
                groupID = profile.primaryGroupID,
                memberID = profile.memberID
            )
        }
    }

    var showMyServices by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showHolidayModePicker by remember { mutableStateOf(false) }
    val appLanguage by LanguageManager.language.collectAsStateWithLifecycle()
    val selectedTab by tabController.selectedTab.collectAsState()
    val isTripActive by ShuttleStore.shared.isTripActive.collectAsStateWithLifecycle()
    val driverLocation by ShuttleStore.shared.driverLocation.collectAsState()
    val driverRoute by ShuttleStore.shared.driverRoute.collectAsState()
    val morningPickups by ShuttleStore.shared.morningPickups.collectAsState()
    val members by ShuttleStore.shared.members.collectAsStateWithLifecycle()

    val showTripAttendanceSheet by viewModel.showTripStartedAttendanceSheet.collectAsState()
    val pendingAttendance by viewModel.pendingAttendanceStatus.collectAsState()
    val isUpdatingAttendance by viewModel.isUpdatingAttendance.collectAsState()
    val isSavingPickup by viewModel.isSavingPickup.collectAsState()
    val draftCoordinate by viewModel.draftPickupCoordinate.collectAsState()

    val myMember = members.firstOrNull { it.id == profile.memberID }
    val myAttendance = myMember?.effectiveAttendance() ?: AttendanceStatus.Unknown
    val isBoardedToday = myMember?.isBoardedToday == true
    val isHolidayModeActive = myMember?.isHolidayModeActive() == true
    val holidayModeSubtitle = remember(myMember?.holidayModeEndDate, isHolidayModeActive) {
        if (isHolidayModeActive) {
            myMember?.holidayModeEndDate
                ?.let { HolidayMode.displayDate(it) }
                ?.let { L10n.holidayModeUntil(it) }
                ?: L10n.holidayModeOff
        } else {
            L10n.holidayModeOff
        }
    }
    val holidayModeDetailLine = remember(myMember?.holidayModeEndDate, isHolidayModeActive) {
        if (isHolidayModeActive) {
            myMember?.holidayModeEndDate
                ?.let { HolidayMode.displayDate(it) }
                ?.let { L10n.holidayModeCardDetailActive(it) }
                ?: L10n.holidayModeCardDetailOff
        } else {
            L10n.holidayModeCardDetailOff
        }
    }

    val savedPickup = remember(morningPickups, profile.memberID) {
        ShuttleStore.shared.morningPickup(profile.memberID)
    }

    var wasTripActive by remember { mutableStateOf(isTripActive) }

    LaunchedEffect(profile.primaryGroupID) {
        MotionActivityService.initialize(context)
        viewModel.onAppear(profile.primaryGroupID)
    }

    LaunchedEffect(profile.primaryGroupID, profile.memberID) {
        updatePassengerMotionMonitoring(
            context = context,
            lifecycleOwner = lifecycleOwner,
            groupID = profile.primaryGroupID,
            memberID = profile.memberID,
            requestPermission = { permission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activityRecognitionLauncher.launch(permission)
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (PushNotificationRouter.consumePendingOpenPassengerMap()) {
            tabController.select(PassengerHomeTab.Map)
        }
        PushNotificationRouter.openPassengerMap.collect {
            tabController.select(PassengerHomeTab.Map)
        }
    }

    val memberLoaded = members.any { it.id == profile.memberID }
    val tripAttendancePromptKey =
        "$isTripActive-${myAttendance.name}-$memberLoaded-${members.size}"

    LaunchedEffect(tripAttendancePromptKey) {
        if (isTripActive && !wasTripActive) {
            viewModel.onTripActiveChanged(false, true, myAttendance)
        } else if (!isTripActive && wasTripActive) {
            viewModel.onTripActiveChanged(true, false, myAttendance)
        }
        wasTripActive = isTripActive
        viewModel.presentTripAttendanceSheetIfNeeded(isTripActive, myAttendance)
    }

    DisposableEffect(lifecycleOwner, tripAttendancePromptKey) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.presentTripAttendanceSheetIfNeeded(isTripActive, myAttendance)
                updatePassengerMotionMonitoring(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    groupID = profile.primaryGroupID,
                    memberID = profile.memberID,
                    requestPermission = { permission ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activityRecognitionLauncher.launch(permission)
                        }
                    }
                )
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                MotionActivityService.stopMonitoring()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            MotionActivityService.stopMonitoring()
        }
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
                            draftLatitude = draftCoordinate?.latitude,
                            draftLongitude = draftCoordinate?.longitude,
                            isUpdatingAttendance = isUpdatingAttendance,
                            isHolidayModeActive = isHolidayModeActive,
                            holidayModeSubtitle = holidayModeSubtitle,
                            holidayModeDetailLine = holidayModeDetailLine,
                            onAttendanceSelected = { status ->
                                viewModel.updateAttendance(status, context)
                            },
                            onOpenHolidayModePicker = { showHolidayModePicker = true },
                            onOpenMap = { tabController.select(PassengerHomeTab.Map) },
                            modifier = Modifier.fillMaxSize()
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
                            myAttendance = myAttendance,
                            isBoardedToday = isBoardedToday,
                            onAttendanceClick = { tabController.select(PassengerHomeTab.Service) },
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
                            currentLanguage = appLanguage,
                            onOpenLanguagePicker = { showLanguagePicker = true },
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
                        driverName = driverLocation?.driverName ?: L10n.driverDefaultName,
                        isLoading = isUpdatingAttendance,
                        pendingStatus = pendingAttendance,
                        onSelectComing = { viewModel.updateAttendance(AttendanceStatus.Coming, context) },
                        onSelectNotComing = { viewModel.updateAttendance(AttendanceStatus.NotComing, context) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            AnimatedVisibility(
                visible = showLanguagePicker,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
            ) {
                LanguagePickerOverlay(
                    selectedLanguage = appLanguage,
                    onSelect = { language ->
                        LanguageManager.setLanguage(context, language)
                        showLanguagePicker = false
                    },
                    onDismiss = { showLanguagePicker = false }
                )
            }

            val isSavingHolidayMode by viewModel.isSavingHolidayMode.collectAsState()
            AnimatedVisibility(
                visible = showHolidayModePicker,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
            ) {
                HolidayModePickerOverlay(
                    isHolidayActive = isHolidayModeActive,
                    activeEndDateKey = myMember?.holidayModeEndDate,
                    isSaving = isSavingHolidayMode,
                    onConfirm = { endDate ->
                        viewModel.saveHolidayMode(endDate) {
                            showHolidayModePicker = false
                        }
                    },
                    onEndHoliday = {
                        viewModel.clearHolidayMode {
                            showHolidayModePicker = false
                        }
                    },
                    onDismiss = { showHolidayModePicker = false }
                )
            }
        }
    }
}

private fun updatePassengerMotionMonitoring(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    groupID: String,
    memberID: String,
    requestPermission: (String) -> Unit
) {
    val isForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    if (!isForeground || groupID.isBlank() || memberID.isBlank()) {
        MotionActivityService.stopMonitoring()
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        !MotionActivityService.hasActivityRecognitionPermission(context)
    ) {
        requestPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        return
    }
    MotionActivityService.updateMonitoring(
        context = context,
        isEnabled = true,
        role = MotionActivityRole.Passenger,
        groupID = groupID,
        memberID = memberID
    )
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
                    text = L10n.live,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NeonTheme.Secondary
                )
            }
        }
    }
}
