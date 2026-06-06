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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mikatechnology.BusTracker.services.NotificationService
import com.mikatechnology.BusTracker.services.PushNotificationRouter
import com.mikatechnology.BusTracker.services.SparseModeSuggestion
import com.mikatechnology.BusTracker.services.SparseModeSuggestionNotifier
import com.mikatechnology.BusTracker.services.BusTrackerAnalytics
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.ui.driver.DriverLocationForegroundGuideSheet
import com.mikatechnology.BusTracker.ui.driver.DriverMotionGuideSheet
import com.mikatechnology.BusTracker.ui.driver.DriverNotificationGuideSheet
import com.mikatechnology.BusTracker.util.openAppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mikatechnology.BusTracker.ui.services.MyServicesScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikatechnology.BusTracker.localization.LanguageManager
import com.mikatechnology.BusTracker.localization.L10n
import com.mikatechnology.BusTracker.ui.settings.LanguagePickerOverlay
import com.mikatechnology.BusTracker.ui.shared.RoleNavBar
import com.mikatechnology.BusTracker.ui.theme.NeonTheme

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.ManagedActivityResultLauncher

private class PermissionLauncherHolder {
    var notificationLauncher: ManagedActivityResultLauncher<String, Boolean>? = null
    var locationLauncher: ManagedActivityResultLauncher<String, Boolean>? = null
    var motionLauncher: ManagedActivityResultLauncher<String, Boolean>? = null

    fun launchNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun launchLocation() {
        locationLauncher?.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun launchMotion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            motionLauncher?.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
}

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
    val scope = rememberCoroutineScope()

    // İzin yönetimi
    val permissionManager = remember { PassengerActionPermissionManager() }
    val activeActionPermissionSheet by permissionManager.activeSheet.collectAsState()
    var notificationWaitingSettings by remember { mutableStateOf(false) }
    var locationWaitingSettings by remember { mutableStateOf(false) }
    var motionWaitingSettings by remember { mutableStateOf(false) }

    // Launcher referansları (sonra atanacak)
    val launcherHolder = remember { PermissionLauncherHolder() }

    val passengerNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionManager.onNotificationPermissionResult(
            context, granted,
            launcherHolder::launchNotification,
            launcherHolder::launchLocation,
            launcherHolder::launchMotion
        ) { action ->
            when (action) {
                PassengerPendingGatedAction.SavePickup -> viewModel.saveMorningPickup(context)
                is PassengerPendingGatedAction.UpdateAttendance -> viewModel.updateAttendance(action.status, context)
            }
        }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionManager.onLocationPermissionResult(
            context,
            launcherHolder::launchNotification,
            launcherHolder::launchLocation,
            launcherHolder::launchMotion
        ) { action ->
            when (action) {
                PassengerPendingGatedAction.SavePickup -> viewModel.saveMorningPickup(context)
                is PassengerPendingGatedAction.UpdateAttendance -> viewModel.updateAttendance(action.status, context)
            }
        }
    }

    val mapLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Harita sekmesi konum isteği - akışa devam etme, sadece refresh
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
        if (LocationTracker.hasFineLocation(context)) {
            LocationTracker.requestSingleLocation(context)
        }
    }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionManager.onMotionPermissionResult(
            context,
            launcherHolder::launchNotification,
            launcherHolder::launchLocation,
            launcherHolder::launchMotion
        ) { action ->
            when (action) {
                PassengerPendingGatedAction.SavePickup -> viewModel.saveMorningPickup(context)
                is PassengerPendingGatedAction.UpdateAttendance -> viewModel.updateAttendance(action.status, context)
            }
        }
    }

    // Launcher'ları holder'a ata
    LaunchedEffect(Unit) {
        launcherHolder.notificationLauncher = passengerNotificationLauncher
        launcherHolder.locationLauncher = fineLocationLauncher
        launcherHolder.motionLauncher = activityRecognitionLauncher
    }

    val googleDeleteAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.deleteAccount(context, result.data)
    }

    var showMyServices by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showHolidayModePicker by remember { mutableStateOf(false) }
    var showSparseModeSuggestionSheet by remember { mutableStateOf(false) }
    var sparseModeComingDays by remember { mutableIntStateOf(0) }
    var showComingBlockedWithoutPickupHint by remember { mutableStateOf(false) }
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
    val isHolidayModeActive = myMember?.isHolidayModeActive() == true
    val planningAttendanceDateKey = remember(isHolidayModeActive) {
        ShuttleStore.shared.planningAttendanceDateKey(isHolidayModeActive)
    }
    val attendanceRevision by ShuttleStore.shared.attendanceRevision.collectAsStateWithLifecycle()
    val myRawAttendance = remember(attendanceRevision, planningAttendanceDateKey, profile.memberID) {
        ShuttleStore.shared.rawAttendanceFor(profile.memberID, planningAttendanceDateKey)
    }
    val myEffectiveAttendance = remember(attendanceRevision, planningAttendanceDateKey, profile.memberID, isHolidayModeActive) {
        ShuttleStore.shared.effectiveAttendanceFor(profile.memberID, planningAttendanceDateKey)
    }
    val isBoardedToday = myMember?.isBoardedToday == true
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

    val attendanceQuestion = remember(isHolidayModeActive, planningAttendanceDateKey) {
        if (isHolidayModeActive) {
            val dateLabel = HolidayMode.displayDateLabel(planningAttendanceDateKey)
            L10n.attendanceQuestionForDate("BUGÜN · $dateLabel")
        } else {
            L10n.attendanceTodayQuestion
        }
    }

    val savedPickup = remember(morningPickups, profile.memberID) {
        ShuttleStore.shared.morningPickup(profile.memberID)
    }
    val hasSavedMorningPickup = savedPickup != null

    LaunchedEffect(hasSavedMorningPickup) {
        if (hasSavedMorningPickup) {
            showComingBlockedWithoutPickupHint = false
        }
    }

    fun onPermissionFlowComplete(action: PassengerPendingGatedAction) {
        when (action) {
            PassengerPendingGatedAction.SavePickup -> viewModel.saveMorningPickup(context)
            is PassengerPendingGatedAction.UpdateAttendance -> viewModel.updateAttendance(action.status, context)
        }
    }

    fun requestComingAttendance() {
        if (!hasSavedMorningPickup) {
            showComingBlockedWithoutPickupHint = true
            tabController.select(PassengerHomeTab.Service)
            viewModel.dismissTripAttendanceSheet()
            return
        }
        showComingBlockedWithoutPickupHint = false
        permissionManager.beginGatedAction(
            context,
            PassengerPendingGatedAction.UpdateAttendance(AttendanceStatus.Coming),
            launcherHolder::launchNotification, launcherHolder::launchLocation, launcherHolder::launchMotion,
            ::onPermissionFlowComplete
        )
    }

    fun requestNotComingAttendance() {
        showComingBlockedWithoutPickupHint = false
        permissionManager.beginGatedAction(
            context,
            PassengerPendingGatedAction.UpdateAttendance(AttendanceStatus.NotComing),
            launcherHolder::launchNotification, launcherHolder::launchLocation, launcherHolder::launchMotion,
            ::onPermissionFlowComplete
        )
    }

    fun requestSaveMorningPickup() {
        if (draftCoordinate == null) {
            viewModel.showPickupCoordinateError()
            return
        }
        permissionManager.beginGatedAction(
            context,
            PassengerPendingGatedAction.SavePickup,
            launcherHolder::launchNotification, launcherHolder::launchLocation, launcherHolder::launchMotion,
            ::onPermissionFlowComplete
        )
    }

    var wasTripActive by remember { mutableStateOf(isTripActive) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == PassengerHomeTab.Map) {
            PassengerActionPermissionManager.promptMapTabLocationIfNeeded(context) {
                mapLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    LaunchedEffect(activeActionPermissionSheet) {
        if (activeActionPermissionSheet != null) {
            notificationWaitingSettings = false
            locationWaitingSettings = false
            motionWaitingSettings = false
        }
    }

    LaunchedEffect(profile.primaryGroupID) {
        MotionActivityService.initialize(context)
        viewModel.onAppear(profile.primaryGroupID)
    }

    LaunchedEffect(profile.primaryGroupID, profile.memberID) {
        updatePassengerMotionMonitoring(
            context = context,
            lifecycleOwner = lifecycleOwner,
            groupID = profile.primaryGroupID,
            memberID = profile.memberID
        )
    }

    val memberLoaded = members.any { it.id == profile.memberID }
    val groupID = profile.primaryGroupID.ifBlank { profile.groupID }

    fun syncTripAttendanceFromStore() {
        val store = ShuttleStore.shared
        if (store.members.value.none { it.id == profile.memberID }) return
        val holiday = store.members.value
            .firstOrNull { it.id == profile.memberID }
            ?.isHolidayModeActive() == true
        val dateKey = store.planningAttendanceDateKey(holiday)
        viewModel.syncTripAttendanceState(
            isTripActive = store.isTripActive.value,
            holidayModeActive = holiday,
            rawAttendance = store.rawAttendanceFor(profile.memberID, dateKey)
        )
    }

    LaunchedEffect(Unit) {
        if (PushNotificationRouter.consumePendingOpenPassengerMap()) {
            tabController.select(PassengerHomeTab.Map)
            syncTripAttendanceFromStore()
        }
        if (PushNotificationRouter.consumePendingOpenSparseModeSheet()) {
            tabController.select(PassengerHomeTab.Service)
            val prompt = SparseModeSuggestion.evaluate(
                context = context,
                groupID = groupID,
                memberID = profile.memberID,
                holidayModeActive = isHolidayModeActive
            )
            if (prompt != null) {
                sparseModeComingDays = prompt.comingDays
                showSparseModeSuggestionSheet = true
            }
        }
        PushNotificationRouter.openPassengerMap.collect {
            tabController.select(PassengerHomeTab.Map)
            syncTripAttendanceFromStore()
        }
        PushNotificationRouter.openSparseModeSheet.collect {
            tabController.select(PassengerHomeTab.Service)
            val prompt = SparseModeSuggestion.evaluate(
                context = context,
                groupID = groupID,
                memberID = profile.memberID,
                holidayModeActive = isHolidayModeActive
            )
            if (prompt != null) {
                sparseModeComingDays = prompt.comingDays
                showSparseModeSuggestionSheet = true
            }
        }
    }

    LaunchedEffect(memberLoaded, isHolidayModeActive, profile.memberID, groupID) {
        if (!memberLoaded || groupID.isBlank()) return@LaunchedEffect
        delay(1_200)
        val prompt = SparseModeSuggestion.evaluate(
            context = context,
            groupID = groupID,
            memberID = profile.memberID,
            holidayModeActive = isHolidayModeActive
        ) ?: return@LaunchedEffect
        sparseModeComingDays = prompt.comingDays
        showSparseModeSuggestionSheet = true
        SparseModeSuggestionNotifier.postNotificationIfNeeded(
            context = context,
            memberID = profile.memberID,
            prompt = prompt
        )
    }
    val tripAttendancePromptKey =
        "$isTripActive-${myRawAttendance.name}-$memberLoaded-$isHolidayModeActive-${members.size}-$attendanceRevision"

    LaunchedEffect(tripAttendancePromptKey) {
        if (isTripActive && !wasTripActive) {
            viewModel.onTripActiveChanged(
                wasActive = false,
                isActive = true,
                attendance = myRawAttendance,
                holidayModeActive = isHolidayModeActive
            )
        } else if (!isTripActive && wasTripActive) {
            viewModel.onTripActiveChanged(
                wasActive = true,
                isActive = false,
                attendance = myRawAttendance,
                holidayModeActive = isHolidayModeActive
            )
        }
        wasTripActive = isTripActive
        if (!memberLoaded) return@LaunchedEffect
        delay(350)
        viewModel.syncTripAttendanceState(
            isTripActive = isTripActive,
            holidayModeActive = isHolidayModeActive,
            rawAttendance = myRawAttendance
        )
    }

    DisposableEffect(lifecycleOwner, tripAttendancePromptKey) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncTripAttendanceState(
                    isTripActive = isTripActive,
                    holidayModeActive = isHolidayModeActive,
                    rawAttendance = myRawAttendance
                )
                LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Passenger)
                MotionActivityService.refreshAuthorization(context)
                if (notificationWaitingSettings || locationWaitingSettings || motionWaitingSettings) {
                    notificationWaitingSettings = false
                    locationWaitingSettings = false
                    motionWaitingSettings = false
                }
                permissionManager.onPermissionsUpdated(
                    context,
                    launcherHolder::launchNotification,
                    launcherHolder::launchLocation,
                    launcherHolder::launchMotion,
                    ::onPermissionFlowComplete
                )
                updatePassengerMotionMonitoring(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    groupID = profile.primaryGroupID,
                    memberID = profile.memberID
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
                            myAttendance = myRawAttendance,
                            myEffectiveAttendance = myEffectiveAttendance,
                            attendanceQuestion = attendanceQuestion,
                            savedMorningPickup = savedPickup,
                            draftLatitude = draftCoordinate?.latitude,
                            draftLongitude = draftCoordinate?.longitude,
                            isUpdatingAttendance = isUpdatingAttendance,
                            isHolidayModeActive = isHolidayModeActive,
                            holidayModeSubtitle = holidayModeSubtitle,
                            holidayModeDetailLine = holidayModeDetailLine,
                            showComingBlockedWithoutPickupHint = showComingBlockedWithoutPickupHint,
                            onAttendanceSelected = { status ->
                                if (status == AttendanceStatus.Coming) {
                                    requestComingAttendance()
                                } else {
                                    requestNotComingAttendance()
                                }
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
                            myAttendance = myEffectiveAttendance,
                            isBoardedToday = isBoardedToday,
                            onAttendanceClick = { tabController.select(PassengerHomeTab.Service) },
                            isTripActive = isTripActive,
                            isSaving = isSavingPickup,
                            onMapClick = { latLng ->
                                viewModel.selectDraftCoordinate(latLng)
                            },
                            onSavePickup = { requestSaveMorningPickup() }
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
                        onSelectComing = { requestComingAttendance() },
                        onSelectNotComing = { requestNotComingAttendance() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            AnimatedVisibility(
                visible = showSparseModeSuggestionSheet,
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
                            .clickable { showSparseModeSuggestionSheet = false }
                    )
                    SparseModeSuggestionSheet(
                        comingDays = sparseModeComingDays,
                        onConfirm = {
                            BusTrackerAnalytics.sparseModePrompt("ok")
                            showSparseModeSuggestionSheet = false
                            tabController.select(PassengerHomeTab.Service)
                            showHolidayModePicker = true
                        },
                        onLater = {
                            BusTrackerAnalytics.sparseModePrompt("later")
                            showSparseModeSuggestionSheet = false
                        },
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
                    .zIndex(6f)
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

            activeActionPermissionSheet?.let { sheet ->
                Dialog(
                    onDismissRequest = { permissionManager.dismissFlow() },
                    properties = DialogProperties(
                        decorFitsSystemWindows = false,
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { permissionManager.dismissFlow() },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        when (sheet) {
                            PassengerActionPermissionSheet.Notification ->
                                DriverNotificationGuideSheet(
                                    waitingForSettingsReturn = notificationWaitingSettings,
                                    bodyGuide = L10n.passengerNotificationPermissionBody,
                                    onOpenSettings = {
                                        notificationWaitingSettings = true
                                        openAppSettings(context)
                                    },
                                    onDismiss = { permissionManager.dismissFlow() }
                                )
                            PassengerActionPermissionSheet.LocationForeground ->
                                DriverLocationForegroundGuideSheet(
                                    waitingForSettingsReturn = locationWaitingSettings,
                                    bodyGuide = L10n.passengerLocationForegroundBody,
                                    onOpenSettings = {
                                        locationWaitingSettings = true
                                        openAppSettings(context)
                                    },
                                    onDismiss = { permissionManager.dismissFlow() }
                                )
                            PassengerActionPermissionSheet.Motion ->
                                DriverMotionGuideSheet(
                                    waitingForSettingsReturn = motionWaitingSettings,
                                    bodyGuide = L10n.passengerMotionPermissionBody,
                                    onOpenSettings = {
                                        motionWaitingSettings = true
                                        openAppSettings(context)
                                    },
                                    onDismiss = { permissionManager.dismissFlow() }
                                )
                        }
                    }
                }
            }
        }
    }
}

private fun updatePassengerMotionMonitoring(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    groupID: String,
    memberID: String
) {
    val isForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    if (!isForeground || groupID.isBlank() || memberID.isBlank()) {
        MotionActivityService.stopMonitoring()
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        !MotionActivityService.hasActivityRecognitionPermission(context)
    ) {
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
