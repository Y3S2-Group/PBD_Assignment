package com.example.financeapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.financeapp.data.local.SettingsRepository
import com.example.financeapp.data.local.SettingsState
import com.example.financeapp.ui.navigation.AppNavigation
import com.example.financeapp.ui.theme.AppTheme
import com.example.financeapp.util.FinancialAlertsScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule the daily 8 PM spending digest notification.
        // setRepeating makes this idempotent — calling it again just updates the alarm.
        FinancialAlertsScheduler.scheduleDailyDigest(applicationContext)

        setContent {
            val settings by settingsRepository.settings.collectAsState(
                initial = SettingsState(
                    isDarkMode = true,
                    language = "en",
                    biometricsEnabled = false,
                    selectedAvatarId = "",
                )
            )
            AppTheme(darkTheme = settings.isDarkMode) {
                AppNavigation(biometricsEnabled = settings.biometricsEnabled)
            }
        }
    }
}
