package com.bitcoinportfolio.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Auth : Screen("auth")
    object Portfolio : Screen("portfolio/{isAuthenticated}") {
        fun createRoute(isAuthenticated: Boolean) = "portfolio/$isAuthenticated"
    }
}
