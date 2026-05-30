package com.mikatechnology.BusTracker.ui.driver

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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikatechnology.BusTracker.auth.GoogleSignInHelper
import com.mikatechnology.BusTracker.base.BaseViewShell
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.model.UserProfile
import com.mikatechnology.BusTracker.data.repository.ShuttleStore
import com.mikatechnology.BusTracker.services.LocationAuthStatus
import com.mikatechnology.BusTracker.services.LocationPermissionRole
import com.mikatechnology.BusTracker.services.LocationTracker
import com.mikatechnology.BusTracker.ui.map.resolveDriverMapLocation
import com.mikatechnology.BusTracker.ui.services.MyServicesScreen
import com.mikatechnology.BusTracker.ui.shared.RoleNavBar
import com.mikatechnology.BusTracker.ui.settings.SettingsDeleteAccountFooter
import com.mikatechnology.BusTracker.ui.settings.SettingsSignOutRow
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import com.mikatechnology.BusTracker.util.openAppSettings

@Composable
fun DriverHomeView(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    viewModel: DriverHomeViewModel = viewModel(
        key = "driver_home_${profile.userID}",
        factory = DriverHomeViewModelFactory(profile)
    ),
    tabController: DriverTabBarController = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val googleDeleteAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.deleteAccount(context, result.data)
    }

    val selectedTab by tabController.selectedTab.collectAsStateWithLifecycle()
    var showMyServices by remember { mutableStateOf(false) }
    var showAlwaysLocationGuide by remember { mutableStateOf(false) }
    var waitingForSettingsReturn by remember { mutableStateOf(false) }

    val members by ShuttleStore.shared.members.collectAsStateWithLifecycle()
    val isTripActive by ShuttleStore.shared.isTripActive.collectAsStateWithLifecycle()
    val driverLocation by ShuttleStore.shared.driverLocation.collectAsStateWithLifecycle()
    val driverRoute by ShuttleStore.shared.driverRoute.collectAsStateWithLifecycle()
    val deviceLocation by LocationTracker.currentLocation.collectAsStateWithLifecycle()
    val morningPickups by ShuttleStore.shared.morningPickups.collectAsStateWithLifecycle()
    val locationAuthStatus by LocationTracker.authorizationStatus.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showTripDurationSheet by viewModel.showTripDurationSheet.collectAsStateWithLifecycle()
    val selectedTripDurationHours by viewModel.selectedTripDurationHours.collectAsStateWithLifecycle()

    val passengers = members.filter { it.role == MemberRole.Passenger }
    val stats = viewModel.passengerStats(members)
    val filteredPickups = viewModel.passengerMorningPickups(passengers, morningPickups)
    val mapDriverLocation = resolveDriverMapLocation(
        firestoreLocation = driverLocation,
        deviceLocation = deviceLocation ?: LocationTracker.effectiveLocation,
        driverName = profile.name
    )
    val canStartTrip = locationAuthStatus == LocationAuthStatus.Always

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
        if (LocationTracker.hasFineLocation(context)) {
            LocationTracker.requestSingleLocation(context)
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
        if (granted) {
            showAlwaysLocationGuide = false
            waitingForSettingsReturn = false
        } else {
            waitingForSettingsReturn = true
        }
    }

    fun requestForegroundLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun requestAlwaysLocationPermission() {
        if (!LocationTracker.hasFineLocation(context)) {
            requestForegroundLocationPermission()
            return
        }
        if (LocationTracker.hasDriverAlwaysLocation(context)) return
        showAlwaysLocationGuide = true
        waitingForSettingsReturn = false
    }

    fun launchAlwaysPermissionFromGuide() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            openAppSettings(context)
            waitingForSettingsReturn = true
        }
    }

    fun refreshDriverLocationAuth() {
        LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
    }

    fun canDriverStartTripNow(): Boolean {
        refreshDriverLocationAuth()
        return LocationTracker.hasDriverAlwaysLocation(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, profile.groupID) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
            if (LocationTracker.hasFineLocation(context)) {
                LocationTracker.requestSingleLocation(context)
            }
            if (LocationTracker.hasDriverAlwaysLocation(context)) {
                showAlwaysLocationGuide = false
                waitingForSettingsReturn = false
            }
        }
    }

    LaunchedEffect(profile.groupID) {
        LocationTracker.initialize(context)
        viewModel.onAppear(profile.groupID)
        if (LocationTracker.hasFineLocation(context)) {
            LocationTracker.refreshAuthorizationStatus(context, LocationPermissionRole.Driver)
            LocationTracker.requestSingleLocation(context)
        }
    }

    BaseViewShell(viewModel = viewModel, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedTab != DriverHomeTab.Map) {
                    DriverTopBar(
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
                        DriverHomeTab.Passengers -> DriverPassengersTab(
                            profile = viewModel.userProfile,
                            passengers = passengers,
                            stats = stats,
                            isTripActive = isTripActive,
                            isTripBusy = uiState.isLoading,
                            locationAuthStatus = locationAuthStatus,
                            onToggleTrip = {
                                val allowed = canDriverStartTripNow()
                                if (!isTripActive && !allowed) {
                                    requestAlwaysLocationPermission()
                                }
                                viewModel.handleTripControlTap(allowed)
                            },
                            onCopyCode = {
                                viewModel.copyGroupCode(context, viewModel.userProfile.groupCode)
                            },
                            onRequestForegroundPermission = { requestForegroundLocationPermission() },
                            onRequestAlwaysPermission = { requestAlwaysLocationPermission() }
                        )

                        DriverHomeTab.Map -> {
                            key("driver_map_tab") {
                                DriverMapTabView(
                                    driverLocation = mapDriverLocation,
                                    driverRoute = driverRoute,
                                    morningPickups = filteredPickups,
                                    stats = stats,
                                    isTripActive = isTripActive
                                )
                            }
                        }

                        DriverHomeTab.Settings -> DriverSettingsTab(
                            profile = viewModel.userProfile,
                            onCopyCode = {
                                viewModel.copyGroupCode(context, viewModel.userProfile.groupCode)
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

                DriverTabBar(
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
                visible = showAlwaysLocationGuide,
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
                            .clickable { showAlwaysLocationGuide = false }
                    )
                    DriverAlwaysLocationGuideSheet(
                        waitingForSettingsReturn = waitingForSettingsReturn,
                        onRequestPermission = { launchAlwaysPermissionFromGuide() },
                        onOpenSettings = {
                            waitingForSettingsReturn = true
                            openAppSettings(context)
                        },
                        onDismiss = { showAlwaysLocationGuide = false },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            AnimatedVisibility(
                visible = showTripDurationSheet,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                            .clickable { viewModel.dismissTripDurationSheet() }
                    )
                    TripDurationBottomSheet(
                        selectedHours = selectedTripDurationHours,
                        onSelectedHoursChange = viewModel::selectTripDurationHours,
                        isLoading = uiState.isLoading,
                        canStartTrip = canDriverStartTripNow(),
                        onConfirm = {
                            val allowed = canDriverStartTripNow()
                            if (!allowed) {
                                requestAlwaysLocationPermission()
                            }
                            viewModel.confirmStartTrip(allowed)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverTopBar(
    isTripActive: Boolean,
    onMenuClick: () -> Unit = {}
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
                    text = "AKTİF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NeonTheme.Secondary
                )
            }
        }
    }
}

@Composable
fun DriverSettingsTab(
    profile: UserProfile,
    onCopyCode: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DriverSettingsRow(
                title = "Servis Kodu",
                value = profile.groupCode,
                onClick = onCopyCode
            )
            DriverSettingsRow(
                title = "Adınız",
                value = profile.name,
                onClick = null
            )
            SettingsSignOutRow(onClick = onSignOut)
        }

        SettingsDeleteAccountFooter(onClick = onDeleteAccount)
    }
}

@Composable
private fun DriverSettingsRow(
    title: String,
    value: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(NeonTheme.SurfaceContainer)
            .border(1.dp, NeonTheme.Outline.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = NeonTheme.OnSurfaceVariant
            )
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold,
                color = NeonTheme.OnSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                tint = NeonTheme.Secondary
            )
        }
    }
}
