package com.example.financeapp.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    /** Device has enrolled fingerprints and hardware is ready. */
    Available,

    /** Hardware exists but no fingerprints are enrolled. */
    NotEnrolled,

    /** Device has no biometric hardware at all. */
    NoHardware,

    /** Hardware exists but is temporarily unavailable. */
    Unavailable,
}

object BiometricHelper {

    private val ALLOWED = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    /**
     * Returns the current biometric availability status on this device.
     * Call from a Composable or Activity — does NOT require a coroutine.
     */
    fun checkStatus(context: Context): BiometricStatus {
        return when (BiometricManager.from(context).canAuthenticate(ALLOWED)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> BiometricStatus.NoHardware
            else -> BiometricStatus.Unavailable
        }
    }

    /**
     * Shows the system biometric prompt attached to [activity].
     *
     * [onSuccess] is called on the main thread when authentication succeeds.
     * [onError] is called for genuine errors (user-cancel and negative-button
     * presses are silently ignored so the caller doesn't have to filter them).
     *
     * Requires the hosting [Activity] to extend [FragmentActivity] — i.e. the app's
     * MainActivity should extend [AppCompatActivity] (which is a [FragmentActivity]).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Vault",
        subtitle: String = "Use your fingerprint to continue",
        negativeButtonText: String = "Use Password",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // User pressed "Use Password" or cancelled — not a real error; ignore.
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                // Fingerprint not recognised — the system already shows retry UI; nothing to do.
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(ALLOWED)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
