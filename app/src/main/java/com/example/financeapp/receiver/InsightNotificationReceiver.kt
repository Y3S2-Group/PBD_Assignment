package com.example.financeapp.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.financeapp.R
import com.example.financeapp.data.local.AnalyticsDatabaseLocator
import com.example.financeapp.ui.dashboard.ReportDetailActivity
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InsightNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> schedule(context)
            ACTION_SHOW_INSIGHT, null -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    showLatestInsight(context)
                    schedule(context)
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun showLatestInsight(context: Context) {
        val latest = AnalyticsDatabaseLocator.get(context).analyticsDao().getLatest()
        createNotificationChannel(context)

        val reportIntent = Intent(context, ReportDetailActivity::class.java)
        val reportPendingIntent = PendingIntent.getActivity(
            context,
            3001,
            reportIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(latest?.primaryInsightTitle ?: context.getString(R.string.analytics_notification_title))
            .setContentText(latest?.primaryInsightMessage ?: context.getString(R.string.analytics_notification_fallback))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(latest?.primaryInsightMessage ?: context.getString(R.string.analytics_notification_fallback))
            )
            .setContentIntent(reportPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.analytics_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_SHOW_INSIGHT = "com.example.financeapp.action.SHOW_INSIGHT"
        private const val CHANNEL_ID = "analytics_insights"
        private const val NOTIFICATION_ID = 4001

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, InsightNotificationReceiver::class.java).apply {
                action = ACTION_SHOW_INSIGHT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                3000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val next = now.withHour(20).withMinute(0).withSecond(0).withNano(0).let {
                if (it.isAfter(now)) it else it.plusDays(1)
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }
}
