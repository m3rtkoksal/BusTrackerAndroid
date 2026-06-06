package com.mikatechnology.BusTracker.ui.registration

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mikatechnology.BusTracker.data.model.MemberRole

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
    val navController = rememberNavController()

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
