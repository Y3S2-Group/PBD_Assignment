package com.example.financeapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules / cancels the daily 8 PM digest alarm using AlarmManager.
 *
 * Mirrors [RecurringIncomeScheduler] — same pattern, no new dependencies.
 *
 * Call [scheduleDailyDigest] from [com.example.financeapp.MainActivity.onCreate];
 * the KEEP semantics of setAndAllowWhileIdle ensure it's safe to call repeatedly.
 */
object FinancialAlertsScheduler {

    private const val DIGEST_HOUR        = 20   // 8 PM local time
    private const val REQUEST_CODE_DAILY = 9001

    /**
     * Sets (or refreshes) a daily 8 PM repeating alarm that fires
     * [FinancialAlertsReceiver] → [FinancialAlertsService.ACTION_DAILY_DIGEST].
     *
     * Uses setRepeating so Android automatically re-fires it every 24 hours.
     */
    fun scheduleDailyDigest(ctx: Context) {
        val triggerAt = nextOccurrenceMillis()
        val pending   = buildPendingIntent(ctx)
        val alarm     = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pending,
        )
    }

    /** Cancels the daily digest alarm (e.g. if the user disables notifications). */
    fun cancelDailyDigest(ctx: Context) {
        val pending = buildPendingIntent(ctx)
        val alarm = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun buildPendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, FinancialAlertsReceiver::class.java).apply {
            action = FinancialAlertsReceiver.ACTION_DAILY_DIGEST_ALARM
        }
        return PendingIntent.getBroadcast(
            ctx,
            REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Returns epoch-millis for the next 8 PM. If it's already past 8 PM today, targets tomorrow. */
    private fun nextOccurrenceMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DIGEST_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
