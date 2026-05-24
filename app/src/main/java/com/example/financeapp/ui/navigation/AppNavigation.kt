package com.example.financeapp.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.ui.auth.AuthViewModel
import com.example.financeapp.ui.auth.LoginScreen
import com.example.financeapp.ui.auth.SignUpScreen
import com.example.financeapp.ui.auth.WelcomeScreen
import com.example.financeapp.util.BiometricHelper
import com.example.financeapp.util.BiometricStatus

@Composable
fun AppNavigation(biometricsEnabled: Boolean = false) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as FragmentActivity

    val currentUser by authViewModel.currentUser.collectAsState()

    val startDestination = remember {
        if (authViewModel.isSignedIn()) AppRoute.MAIN_GRAPH else AppRoute.AUTH_GRAPH
    }

    // ── Biometric session state ────────────────────────────────────────────
    // isUnlocked  — true once the user passed biometric (or came through the normal login flow)
    // bypassLock  — true while navigating to the login screen via "Use Password";
    //               hides the overlay without actually signing out Firebase, so the
    //               login-screen fingerprint button can still call isSignedIn() → true.
    var isUnlocked by remember { mutableStateOf(!authViewModel.isSignedIn()) }
    var bypassLock  by remember { mutableStateOf(false) }

    // Reset both flags when the Firebase user changes (e.g. explicit sign-out)
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            isUnlocked  = false
            bypassLock  = false
        }
    }

    val biometricAvailable = remember(biometricsEnabled) {
        biometricsEnabled && BiometricHelper.checkStatus(context) == BiometricStatus.Available
    }

    // Show the lock overlay only when:
    //  • user is signed in to Firebase
    //  • biometrics is enabled and the hardware is available
    //  • this session hasn't been unlocked yet
    //  • user hasn't chosen "Use Password" (bypass flag)
    val showLock = biometricAvailable && currentUser != null && !isUnlocked && !bypassLock

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Navigation graph ───────────────────────────────────────────────
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            // AUTH graph ───────────────────────────────────────────────────
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
                        biometricsEnabled = biometricsEnabled,
                        onLoginSuccess = {
                            // Fresh authentication — mark unlocked and clear the bypass flag.
                            isUnlocked = true
                            bypassLock  = false
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
                            isUnlocked = true
                            bypassLock  = false
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

            // MAIN graph ───────────────────────────────────────────────────
            navigation(
                route = AppRoute.MAIN_GRAPH,
                startDestination = AppRoute.ROUTE_MAIN_CONTENT,
            ) {
                composable(AppRoute.ROUTE_MAIN_CONTENT) {
                    MainScreen(
                        onSignOut = {
                            // Explicit logout (from the Profile page).
                            // ProfileViewModel.logout() already signed out Firebase and cleared
                            // the biometricsEnabled setting from DataStore; reset local state too.
                            isUnlocked = false
                            bypassLock  = false
                            authViewModel.signOut()
                            navController.navigate(AppRoute.AUTH_GRAPH) {
                                popUpTo(AppRoute.MAIN_GRAPH) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }

        // ── Biometric lock overlay ─────────────────────────────────────────
        AnimatedVisibility(
            visible = showLock,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            BiometricLockScreen(
                onLaunchPrompt = {
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = "Unlock Vault",
                        subtitle = "Use your fingerprint to continue",
                        negativeButtonText = "Use Password",
                        onSuccess = { isUnlocked = true },
                    )
                },
                onUsePassword = {
                    // ⚠️  Do NOT sign Firebase out here.
                    // The session stays alive so the login screen's fingerprint button
                    // will find isSignedIn() == true and can unlock without re-entering
                    // credentials.  bypassLock hides this overlay while on the login screen.
                    bypassLock = true
                    navController.navigate(AppRoute.ROUTE_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

// ── Biometric lock screen UI ──────────────────────────────────────────────────

@Composable
private fun BiometricLockScreen(
    onLaunchPrompt: () -> Unit,
    onUsePassword: () -> Unit,
) {
    // Auto-trigger the system fingerprint prompt when this screen first appears.
    LaunchedEffect(Unit) { onLaunchPrompt() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = "Fingerprint",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Vault is locked",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Verify your identity to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onLaunchPrompt,
                modifier = Modifier.size(width = 220.dp, height = 52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Use Fingerprint",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onUsePassword) {
                Text(
                    text = "Use Password Instead",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
