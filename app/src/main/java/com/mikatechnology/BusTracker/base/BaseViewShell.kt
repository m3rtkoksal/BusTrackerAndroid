package com.mikatechnology.BusTracker.base

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikatechnology.BusTracker.ui.theme.NeonBackground
import com.mikatechnology.BusTracker.ui.theme.NeonTheme
import kotlinx.coroutines.delay

/**
 * Compose shell matching iOS `BaseView` + `BaseViewShell`.
 *
 * Usage:
 * ```
 * BaseViewShell(
 *     viewModel = viewModel,
 *     onBack = { navController.popBackStack() }
 * ) {
 *     // screen content
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseViewShell(
    viewModel: BaseViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingToolbar: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.toast?.id) {
        val toast = state.toast ?: return@LaunchedEffect
        delay(3_000)
        if (viewModel.uiState.value.toast?.id == toast.id) {
            viewModel.clearToast()
        }
    }

    state.alert?.let { popup ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAlert() },
            icon = { PopupIcon(popup.style) },
            title = { Text(popup.title) },
            text = { Text(popup.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAlert() }) {
                    Text("Tamam")
                }
            }
        )
    }

    state.confirmDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { viewModel.clearConfirmDialog() },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDialogAction() }) {
                    Text(
                        text = dialog.confirmTitle,
                        color = if (dialog.isDestructive) NeonTheme.Error else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearConfirmDialog() }) {
                    Text(dialog.cancelTitle)
                }
            }
        )
    }

    val showTopBar = state.embedsInNavigationStack &&
        !state.hidesNavigationBar &&
        !state.showsCustomNavHeader()

    val containerColor = when (state.navigationBarStyle) {
        NavigationBarStyle.Driver -> Color.Blue
        NavigationBarStyle.Passenger -> Color(0xFF2E7D32)
        NavigationBarStyle.NeonAuth,
        NavigationBarStyle.NeonDriver,
        NavigationBarStyle.NeonPassenger -> Color.Transparent
        NavigationBarStyle.Primary,
        NavigationBarStyle.Auth -> MaterialTheme.colorScheme.background
    }

    val contentColor = when (state.navigationBarStyle) {
        NavigationBarStyle.Driver,
        NavigationBarStyle.Passenger -> Color.White
        NavigationBarStyle.NeonAuth,
        NavigationBarStyle.NeonDriver,
        NavigationBarStyle.NeonPassenger -> NeonTheme.OnSurface
        NavigationBarStyle.Primary,
        NavigationBarStyle.Auth -> MaterialTheme.colorScheme.onBackground
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.navigationBarStyle.usesNeonBackground) {
            NeonBackground()
        }

        if (!state.embedsInNavigationStack) {
            ScreenBody(
                state = state,
                viewModel = viewModel,
                content = content
            )
            return@Box
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (state.navigationBarStyle.usesNeonBackground) {
                Color.Transparent
            } else {
                containerColor
            },
            topBar = {
                if (showTopBar) {
                    if (state.usesLargeTitle) {
                        CenterAlignedTopAppBar(
                            title = { Text(state.title) },
                            navigationIcon = {
                                if (state.showsBackButton && onBack != null) {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Geri"
                                        )
                                    }
                                }
                            },
                            actions = { trailingToolbar() },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = containerColor,
                                titleContentColor = contentColor,
                                navigationIconContentColor = contentColor,
                                actionIconContentColor = contentColor
                            )
                        )
                    } else {
                        TopAppBar(
                            title = { Text(state.title) },
                            navigationIcon = {
                                if (state.showsBackButton && onBack != null) {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Geri"
                                        )
                                    }
                                }
                            },
                            actions = { trailingToolbar() },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = containerColor,
                                titleContentColor = contentColor,
                                navigationIconContentColor = contentColor,
                                actionIconContentColor = contentColor
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            ScreenBody(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding),
                content = content
            )
        }
    }
}

@Composable
private fun ScreenBody(
    state: BaseUiState,
    viewModel: BaseViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val showsHeader = state.showsCustomNavHeader()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (showsHeader) {
            BaseNeonNavHeader(
                title = state.title,
                subtitle = state.subtitle,
                subtitleStyle = state.navSubtitleStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.contentScrollEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                ) {
                    content()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    content()
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.toast != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    state.toast?.let { toast ->
                        ToastBanner(
                            popup = toast,
                            onDismiss = { viewModel.clearToast() }
                        )
                    }
                }
            }

            if (state.isLoading) {
                LoadingOverlay(
                    message = state.loadingMessage,
                    usesNeonStyle = state.navigationBarStyle.usesNeonBackground
                )
            }
        }
    }
}

private fun BaseUiState.showsCustomNavHeader(): Boolean {
    return usesCustomNavHeader ?: navigationBarStyle.usesCustomNavHeader
}

@Composable
private fun BaseNeonNavHeader(
    title: String,
    subtitle: String,
    subtitleStyle: NavSubtitleStyle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = NeonTheme.OnSurface,
            textAlign = TextAlign.Center
        )

        if (subtitleStyle != NavSubtitleStyle.Hidden && subtitle.isNotBlank()) {
            when (subtitleStyle) {
                NavSubtitleStyle.NeonCaps -> {
                    Text(
                        text = subtitle.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color = NeonTheme.Secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                NavSubtitleStyle.Standard,
                NavSubtitleStyle.Hidden -> {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonTheme.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingOverlay(
    message: String,
    usesNeonStyle: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (usesNeonStyle) 0.55f else 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = if (usesNeonStyle) {
                        NeonTheme.SurfaceContainerHigh.copy(alpha = 0.96f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = if (usesNeonStyle) NeonTheme.Primary else MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (usesNeonStyle) NeonTheme.OnSurface else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun ToastBanner(
    popup: PopupPresentation,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PopupIcon(popup.style)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = popup.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = popup.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PopupIcon(style: PopupStyle) {
    val (icon, tint) = when (style) {
        PopupStyle.Error -> Icons.Default.Error to NeonTheme.Error
        PopupStyle.Success -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        PopupStyle.Info -> Icons.Default.Info to Color(0xFF2196F3)
        PopupStyle.Warning -> Icons.Default.Warning to Color(0xFFFF9800)
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint
    )
}
