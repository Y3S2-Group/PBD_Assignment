package com.example.financeapp.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.financeapp.FinanceApp
import com.example.financeapp.MainActivity
import com.example.financeapp.R

/**
 * BroadcastReceiver that handles two intents:
 *
 * 1. [ACTION_RECURRING_INCOME_DUE] — fired by AlarmManager when a recurring income is due.
 *    Posts a local notification prompting the user to log the income in the app.
 *
 * 2. [Intent.ACTION_BOOT_COMPLETED] — fired by the OS after device reboot.
 *    Starts [RecurringIncomeReminderService] to re-schedule all active recurring income alarms
 *    (AlarmManager alarms are cleared after a reboot).
 */
class RecurringIncomeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_RECURRING_INCOME_DUE -> postIncomeReminderNotification(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAfterBoot(context)
        }
    }

    // ── Notification posting ──────────────────────────────────────────────────

    private fun postIncomeReminderNotification(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: return
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val currency = intent.getStringExtra(EXTRA_CURRENCY) ?: "LKR"
        val notifId = intent.getStringExtra(EXTRA_ID)?.hashCode() ?: label.hashCode()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = android.app.PendingIntent.getActivity(
            context,
            notifId,
            tapIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, FinanceApp.RECURRING_INCOME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Income Due Today")
            .setContentText("Your $label income of $currency ${formatAmount(amount)} is due. Tap to log it.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $label income of $currency ${formatAmount(amount)} is scheduled for today. Open Vault to log it now.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notifId, notification)
    }

    // ── Boot reschedule ───────────────────────────────────────────────────────

    private fun rescheduleAfterBoot(context: Context) {
        val serviceIntent = Intent(context, RecurringIncomeReminderService::class.java).apply {
            action = RecurringIncomeReminderService.ACTION_RESCHEDULE
        }
        context.startService(serviceIntent)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString()
        else "%.2f".format(amount)

    companion object {
        const val ACTION_RECURRING_INCOME_DUE = "com.example.financeapp.ACTION_RECURRING_INCOME_DUE"
        const val EXTRA_ID = "extra_recurring_id"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_CURRENCY = "extra_currency"
    }
}
