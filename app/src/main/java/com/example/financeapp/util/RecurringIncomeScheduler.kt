package com.example.financeapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.financeapp.domain.model.RecurringIncome
import java.util.Calendar

/**
 * Schedules / cancels AlarmManager alarms for recurring income reminders.
 * Each alarm fires at [REMINDER_HOUR]:00 on the configured [RecurringIncome.dayOfMonth].
 */
object RecurringIncomeScheduler {

    private const val REMINDER_HOUR = 9  // 9 AM local time

    fun schedule(context: Context, recurring: RecurringIncome) {
        val triggerAtMillis = nextOccurrenceMillis(recurring.dayOfMonth)
        val pending = buildPendingIntent(context, recurring, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
    }

    fun cancel(context: Context, recurringId: String) {
        val intent = Intent(context, RecurringIncomeReceiver::class.java).apply {
            action = RecurringIncomeReceiver.ACTION_RECURRING_INCOME_DUE
        }
        val pending = PendingIntent.getBroadcast(
            context,
            recurringId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pending?.let {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(it)
            it.cancel()
        }
    }

    private fun buildPendingIntent(
        context: Context,
        recurring: RecurringIncome,
        extraFlags: Int,
    ): PendingIntent {
        val intent = Intent(context, RecurringIncomeReceiver::class.java).apply {
            action = RecurringIncomeReceiver.ACTION_RECURRING_INCOME_DUE
            putExtra(RecurringIncomeReceiver.EXTRA_ID, recurring.id)
            putExtra(
                RecurringIncomeReceiver.EXTRA_LABEL,
                recurring.sourceLabel ?: recurring.sourceType
            )
            putExtra(RecurringIncomeReceiver.EXTRA_AMOUNT, recurring.defaultAmount)
            putExtra(RecurringIncomeReceiver.EXTRA_CURRENCY, recurring.currency)
        }
        return PendingIntent.getBroadcast(
            context,
            recurring.id.hashCode(),
            intent,
            extraFlags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Returns epoch-millis for the next occurrence of [dayOfMonth] at [REMINDER_HOUR]:00. */
    private fun nextOccurrenceMillis(dayOfMonth: Int): Long {
        val day = dayOfMonth.coerceIn(1, 28)
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If this month's occurrence is already past, schedule for next month.
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }
}
