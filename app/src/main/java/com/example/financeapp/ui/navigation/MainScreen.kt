package com.example.financeapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.ui.budget.BudgetScreen
import com.example.financeapp.ui.dashboard.DashboardScreen
import com.example.financeapp.ui.expense.ExpenseScreen
import com.example.financeapp.ui.income.IncomeScreen
import com.example.financeapp.ui.notifications.NotificationScreen
import com.example.financeapp.ui.profile.ProfileScreen

@Composable
fun MainScreen(
    onSignOut: () -> Unit = {},
    avatarId: String,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val hideBottomBar = currentDestination?.hierarchy?.any {
        it.route == AppRoute.Profile.route || it.route == AppRoute.Notifications.route
    } == true

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ) {
                    AppRoute.bottomNavItems.forEach { item ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(AppRoute.Dashboard.route) {
                                            saveState = true
                                        }
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(text = item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledIconColor = Color.Transparent,
                                disabledTextColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Dashboard.route) {
                DashboardScreen(
                    avatarId = avatarId,
                    onProfileClick = { navController.navigate(AppRoute.Profile.route) },
                    onNotificationClick = { navController.navigate(AppRoute.Notifications.route) }
                )
            }
            composable(AppRoute.Income.route) {
                IncomeScreen(
                    avatarId = avatarId,
                    onProfileClick = { navController.navigate(AppRoute.Profile.route) },
                    onNotificationClick = { navController.navigate(AppRoute.Notifications.route) }
                )
            }
            composable(AppRoute.Expenses.route) {
                ExpenseScreen(
                    avatarId = avatarId,
                    onProfileClick = { navController.navigate(AppRoute.Profile.route) },
                    onNotificationClick = { navController.navigate(AppRoute.Notifications.route) }
                )
            }
            composable(AppRoute.Budget.route) {
                BudgetScreen(
                    avatarId = avatarId,
                    onProfileClick = { navController.navigate(AppRoute.Profile.route) },
                    onNotificationClick = { navController.navigate(AppRoute.Notifications.route) }
                )
            }
            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    onLogout = onSignOut,
                    onBack = { navController.navigateUp() },
                )
            }
            composable(AppRoute.Notifications.route) {
                NotificationScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}
