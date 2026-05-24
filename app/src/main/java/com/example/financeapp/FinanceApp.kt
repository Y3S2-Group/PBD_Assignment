package com.example.financeapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.financeapp.util.VaultNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinanceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Existing channel — recurring income reminders
            manager.createNotificationChannel(
                NotificationChannel(
                    RECURRING_INCOME_CHANNEL_ID,
                    "Recurring Income Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Daily reminders when a recurring income entry is due"
                }
            )

            // Financial alerts — high importance (vibrate, heads-up)
            manager.createNotificationChannel(
                NotificationChannel(
                    VaultNotificationHelper.CHANNEL_ALERTS,
                    "Financial Alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Urgent alerts: budget exceeded, expenses over income, large transactions"
                    enableVibration(true)
                }
            )

            // Goal milestones & savings streaks
            manager.createNotificationChannel(
                NotificationChannel(
                    VaultNotificationHelper.CHANNEL_MILESTONES,
                    "Goal Milestones",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Savings goal milestones, deadline reminders, and streak achievements"
                }
            )

            // Daily digest — silent, low priority
            manager.createNotificationChannel(
                NotificationChannel(
                    VaultNotificationHelper.CHANNEL_DAILY,
                    "Daily Digest",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Daily summary of your spending vs. budget, delivered at 8 PM"
                    setSound(null, null)
                }
            )
        }
    }

    companion object {
        const val RECURRING_INCOME_CHANNEL_ID = "recurring_income_reminders"
    }
}
