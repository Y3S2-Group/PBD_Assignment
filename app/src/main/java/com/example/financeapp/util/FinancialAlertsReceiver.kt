package com.example.financeapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver for two events:
 *
 * 1. [ACTION_DAILY_DIGEST_ALARM] — fired by AlarmManager at 8 PM.
 *    Starts [FinancialAlertsService] in daily-digest mode.
 *
 * 2. [Intent.ACTION_BOOT_COMPLETED] — fires after device reboot.
 *    Re-schedules the daily 8 PM alarm (AlarmManager alarms are cleared on reboot).
 *
 * No Hilt injection needed here — it simply starts the service.
 */
class FinancialAlertsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DAILY_DIGEST_ALARM  -> startDailyDigestService(context)
            Intent.ACTION_BOOT_COMPLETED -> rescheduleDailyAlarm(context)
        }
    }

    private fun startDailyDigestService(ctx: Context) {
        ctx.startService(
            Intent(ctx, FinancialAlertsService::class.java).apply {
                action = FinancialAlertsService.ACTION_DAILY_DIGEST
            }
        )
    }

    private fun rescheduleDailyAlarm(ctx: Context) {
        FinancialAlertsScheduler.scheduleDailyDigest(ctx)
    }

    companion object {
        const val ACTION_DAILY_DIGEST_ALARM = "com.example.financeapp.ACTION_DAILY_DIGEST_ALARM"
    }
}
