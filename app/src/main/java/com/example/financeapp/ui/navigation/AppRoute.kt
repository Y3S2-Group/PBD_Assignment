package com.example.financeapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Dashboard : AppRoute("dashboard", "Dashboard", Icons.Rounded.Dashboard)
    data object Income : AppRoute("income", "Income", Icons.Rounded.Payments)
    data object Expenses : AppRoute("expenses", "Expenses", Icons.AutoMirrored.Rounded.ReceiptLong)
    data object Budget : AppRoute("budget", "Budget", Icons.Rounded.AccountBalanceWallet)

    companion object {
        val bottomNavItems = listOf(Dashboard, Income, Budget, Expenses)
    }
}


