package com.example.financeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.financeapp.receiver.InsightNotificationReceiver
import com.example.financeapp.service.AnalyticsComputeService
import com.example.financeapp.ui.navigation.MainScreen
import com.example.financeapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsComputeService.enqueue(this)
        InsightNotificationReceiver.schedule(this)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}
