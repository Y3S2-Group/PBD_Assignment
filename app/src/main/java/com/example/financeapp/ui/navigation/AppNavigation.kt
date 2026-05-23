package com.example.financeapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.ui.auth.AuthViewModel
import com.example.financeapp.ui.auth.LoginScreen
import com.example.financeapp.ui.auth.SignUpScreen
import com.example.financeapp.ui.auth.WelcomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val startDestination = remember {
        if (authViewModel.isSignedIn()) AppRoute.MAIN_GRAPH else AppRoute.AUTH_GRAPH
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ── Auth graph ────────────────────────────────────────────────
        navigation(
            route = AppRoute.AUTH_GRAPH,
            startDestination = AppRoute.ROUTE_WELCOME,
        ) {
            composable(AppRoute.ROUTE_WELCOME) {
                WelcomeScreen(
                    onGetStarted = { navController.navigate(AppRoute.ROUTE_SIGNUP) },
                    onSignIn = { navController.navigate(AppRoute.ROUTE_LOGIN) },
                )
            }

            composable(AppRoute.ROUTE_LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(AppRoute.MAIN_GRAPH) {
                            popUpTo(AppRoute.AUTH_GRAPH) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(AppRoute.ROUTE_SIGNUP)
                    },
                )
            }

            composable(AppRoute.ROUTE_SIGNUP) {
                SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate(AppRoute.MAIN_GRAPH) {
                            popUpTo(AppRoute.AUTH_GRAPH) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(AppRoute.ROUTE_LOGIN) {
                            popUpTo(AppRoute.ROUTE_WELCOME)
                        }
                    },
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }

        // ── Main graph ────────────────────────────────────────────────
        navigation(
            route = AppRoute.MAIN_GRAPH,
            startDestination = AppRoute.ROUTE_MAIN_CONTENT,
        ) {
            composable(AppRoute.ROUTE_MAIN_CONTENT) {
                MainScreen(
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(AppRoute.AUTH_GRAPH) {
                            popUpTo(AppRoute.MAIN_GRAPH) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
