package com.example.financeapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppRoute(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Dashboard : AppRoute("dashboard", "Dashboard", Icons.Rounded.Dashboard)
    data object Income : AppRoute("income", "Income", Icons.Rounded.Payments)
    data object Expenses : AppRoute("expenses", "Expenses", Icons.AutoMirrored.Rounded.ReceiptLong)
    data object Budget : AppRoute("budget", "Budget", Icons.Rounded.AccountBalanceWallet)
    data object Profile : AppRoute("profile", "Profile", Icons.Rounded.Person)
    data object Notifications : AppRoute("notifications", "Notifications", Icons.Rounded.Notifications)

    companion object {
        // Navigation graph roots
        const val AUTH_GRAPH = "auth_graph"
        const val MAIN_GRAPH = "main_graph"

        // Auth screen routes
        const val ROUTE_WELCOME = "welcome"
        const val ROUTE_LOGIN = "login"
        const val ROUTE_SIGNUP = "signup"

        // Main content wrapper route
        const val ROUTE_MAIN_CONTENT = "main_content"

        val bottomNavItems = listOf(Dashboard, Income, Budget, Expenses)
    }
}
