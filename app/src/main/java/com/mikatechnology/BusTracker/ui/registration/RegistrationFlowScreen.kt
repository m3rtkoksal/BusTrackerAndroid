package com.mikatechnology.BusTracker.ui.registration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mikatechnology.BusTracker.data.model.MemberRole
import com.mikatechnology.BusTracker.data.smler.SmlerDeepLinkService
import com.mikatechnology.BusTracker.data.smler.SmlerInviteCoordinator
import com.mikatechnology.BusTracker.data.smler.SmlerPendingInviteURL

object RegistrationRoutes {
    const val RoleSelection = "role_selection"
    const val RegistrationForm = "registration/{role}"

    fun form(role: MemberRole): String = "registration/${role.rawValue}"
}

@Composable
fun RegistrationFlowScreen(
    onLoginTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val inviteRevision by SmlerInviteCoordinator.inviteRevision.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        processDeferredSmlerInviteIfNeeded(context, navController)
    }

    LaunchedEffect(inviteRevision) {
        openPassengerRegistrationIfInvited(navController)
    }

    NavHost(
        navController = navController,
        startDestination = RegistrationRoutes.RoleSelection,
        modifier = modifier
    ) {
        composable(RegistrationRoutes.RoleSelection) {
            RoleSelectionScreen(
                onLoginTapped = onLoginTapped,
                onSelectDriver = {
                    navController.navigate(RegistrationRoutes.form(MemberRole.Driver))
                },
                onSelectPassenger = {
                    SmlerInviteCoordinator.preparePassengerRegistrationFromDeferred()
                    navController.navigate(RegistrationRoutes.form(MemberRole.Passenger))
                }
            )
        }

        composable(
            route = RegistrationRoutes.RegistrationForm,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { entry ->
            val role = MemberRole.fromRoute(entry.arguments?.getString("role").orEmpty())
            RegistrationFormScreen(
                role = role,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private suspend fun processDeferredSmlerInviteIfNeeded(
    context: android.content.Context,
    navController: NavHostController
) {
    SmlerInviteCoordinator.restorePersistedRegistrationInviteIfNeeded(context)

    SmlerPendingInviteURL.consume()?.let { uri ->
        SmlerInviteCoordinator.processIncomingURL(context, uri)
    }

    if (SmlerInviteCoordinator.hasPassengerRegistrationInvite()) {
        openPassengerRegistrationIfInvited(navController)
        return
    }

    SmlerDeepLinkService.serviceCodeFromDeferredInstall()?.let { code ->
        SmlerInviteCoordinator.ingest(context, code)
    }
    openPassengerRegistrationIfInvited(navController)
}

private fun openPassengerRegistrationIfInvited(navController: NavHostController) {
    if (!SmlerInviteCoordinator.hasPassengerRegistrationInvite()) return
    SmlerInviteCoordinator.preparePassengerRegistrationFromDeferred()
    val currentRoute = navController.currentDestination?.route
    if (currentRoute == RegistrationRoutes.RoleSelection) {
        navController.navigate(RegistrationRoutes.form(MemberRole.Passenger))
    }
}
