package com.example.financeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.financeapp.data.local.SettingsRepository
import com.example.financeapp.data.local.SettingsState
import com.example.financeapp.ui.navigation.AppNavigation
import com.example.financeapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(
                initial = SettingsState(
                    isDarkMode = true,
                    language = "en",
                    currency = "LKR",
                    biometricsEnabled = false,
                    localProfilePhotoPath = "",
                )
            )
            AppTheme(darkTheme = settings.isDarkMode) {
                AppNavigation()
            }
        }
    }
}
