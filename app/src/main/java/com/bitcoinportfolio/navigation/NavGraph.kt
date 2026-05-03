package com.bitcoinportfolio.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bitcoinportfolio.ui.auth.AuthScreen
import com.bitcoinportfolio.ui.portfolio.PortfolioScreen
import com.bitcoinportfolio.ui.setup.SetupScreen

@Composable
fun NavGraph(startDestination: String = Screen.Setup.route) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthResult = { isAuthenticated ->
                    navController.navigate(Screen.Portfolio.createRoute(isAuthenticated)) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onReset = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Portfolio.route,
            arguments = listOf(
                navArgument("isAuthenticated") { type = NavType.BoolType }
            )
        ) {
            PortfolioScreen()
        }
    }
}
