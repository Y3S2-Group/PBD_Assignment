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

    // Observe the live Firebase user so we react to sign-in / sign-out.
    val currentUser by authViewModel.currentUser.collectAsState()

    val startDestination = remember {
        if (authViewModel.isSignedIn()) AppRoute.MAIN_GRAPH else AppRoute.AUTH_GRAPH
    }

    // ── Biometric unlock tracking ──────────────────────────────────────────
    // Starts "locked" when the app launches with an existing Firebase session;
    // starts "unlocked" when there is no session (user will authenticate via
    // the normal login flow, which counts as a fresh authentication).
    var isUnlocked by remember { mutableStateOf(!authViewModel.isSignedIn()) }

    // Reset whenever the signed-in user changes (e.g. after sign-out / sign-in cycle).
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) isUnlocked = false
    }

    val biometricAvailable =
        remember(biometricsEnabled) {
            biometricsEnabled && BiometricHelper.checkStatus(context) == BiometricStatus.Available
        }

    // Show the lock screen only when: signed in + biometrics on + not yet unlocked this session.
    val showLock = biometricAvailable && currentUser != null && !isUnlocked

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Main navigation graph ──────────────────────────────────────────
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
                            // User just authenticated — no need to biometric-gate this session.
                            isUnlocked = true
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
                            isUnlocked = false
                            authViewModel.signOut()
                            navController.navigate(AppRoute.AUTH_GRAPH) {
                                popUpTo(AppRoute.MAIN_GRAPH) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }

        // ── Biometric lock overlay — rendered on top of everything ─────────
        AnimatedVisibility(
            visible = showLock,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            BiometricLockScreen(
                onUnlocked = { isUnlocked = true },
                onUsePassword = {
                    // Sign the user out so they must re-enter credentials.
                    // isUnlocked stays false; after a fresh login onLoginSuccess sets it to true.
                    authViewModel.signOut()
                    navController.navigate(AppRoute.AUTH_GRAPH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLaunchPrompt = {
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = "Unlock Vault",
                        subtitle = "Use your fingerprint to continue",
                        negativeButtonText = "Use Password",
                        onSuccess = { isUnlocked = true },
                    )
                },
            )
        }
    }
}

// ── Biometric lock screen ──────────────────────────────────────────────────

@Composable
private fun BiometricLockScreen(
    onUnlocked: () -> Unit,
    onUsePassword: () -> Unit,
    onLaunchPrompt: () -> Unit,
) {
    // Automatically trigger the biometric prompt as soon as this screen appears.
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
            // Fingerprint icon badge
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

            // "Use Fingerprint" button — re-triggers the system prompt
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

            // Fall-back — signs the user out and returns to the login screen
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
