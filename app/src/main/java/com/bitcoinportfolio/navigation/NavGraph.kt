package com.bitcoinportfolio.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bitcoinportfolio.ui.auth.AuthScreen
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
            PlaceholderScreen("Portfolio")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$name Screen",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
