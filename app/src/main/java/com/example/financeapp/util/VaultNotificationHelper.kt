package com.example.financeapp.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import com.example.financeapp.MainActivity
import com.example.financeapp.R

/**
 * Central utility for building and posting Vault financial push notifications.
 *
 * Every alert method checks a per-key 24-hour cooldown stored in SharedPreferences
 * before posting, so the same alert cannot fire more than once a day.
 */
object VaultNotificationHelper {

    // ── Stable notification IDs ─────────────────────────────────────────────────
    private const val ID_OVERSHOOT       = 1001
    private const val ID_LARGE_EXPENSE   = 1002
    // Budget IDs are computed per-category: ID_BUDGET_BASE + category.hashCode()
    private const val ID_BUDGET_EXCEEDED_BASE = 2000
    private const val ID_BUDGET_WARNING_BASE  = 3000
    private const val ID_GOAL_DEADLINE   = 4001
    private const val ID_GOAL_MILESTONE  = 4002
    private const val ID_DAILY_DIGEST    = 5001
    private const val ID_SAVINGS_STREAK  = 6001

    // ── Cooldown ────────────────────────────────────────────────────────────────
    private const val PREFS_NAME   = "vault_notif_cooldown"
    private const val COOLDOWN_MS  = 24L * 60 * 60 * 1_000   // 24 hours

    // ── Channel IDs (declared here, registered in FinanceApp) ──────────────────
    const val CHANNEL_ALERTS     = "vault_alerts"
    const val CHANNEL_MILESTONES = "vault_milestones"
    const val CHANNEL_DAILY      = "vault_daily"

    // ── Public notification methods ─────────────────────────────────────────────

    /**
     * Alert when monthly expenses exceed monthly income.
     * @param expensePct e.g. 107 means spending is 107% of income
     */
    fun postOverspendAlert(ctx: Context, expensePct: Int) {
        val key = "overshoot"
        if (!cooldownPassed(ctx, key)) return
        val over = expensePct - 100
        post(
            ctx       = ctx,
            channelId = CHANNEL_ALERTS,
            notifId   = ID_OVERSHOOT,
            title     = "🚨 Expenses Exceed Income!",
            body      = "You're spending $over% more than you earn this month. Review your budget now to avoid a deficit.",
            priority  = NotificationCompat.PRIORITY_HIGH,
        )
        markPosted(ctx, key)
    }

    /**
     * Alert when a single expense is ≥ 15% of monthly income.
     */
    fun postLargeExpense(ctx: Context, category: String, amountLkr: Double, incomePct: Int) {
        val key = "large_expense_${amountLkr.toLong()}"
        if (!cooldownPassed(ctx, key)) return
        post(
            ctx       = ctx,
            channelId = CHANNEL_ALERTS,
            notifId   = ID_LARGE_EXPENSE,
            title     = "💸 Large Expense: ${fmtLkr(amountLkr)}",
            body      = "$category — this single expense is $incomePct% of your monthly income.",
            priority  = NotificationCompat.PRIORITY_HIGH,
        )
        markPosted(ctx, key)
    }

    /**
     * Alert when spending in [category] has exceeded its allocated budget.
     */
    fun postBudgetExceeded(ctx: Context, category: String, spentLkr: Double, limitLkr: Double) {
        val key = "budget_exceeded_$category"
        if (!cooldownPassed(ctx, key)) return
        val over = spentLkr - limitLkr
        post(
            ctx       = ctx,
            channelId = CHANNEL_ALERTS,
            notifId   = ID_BUDGET_EXCEEDED_BASE + (category.hashCode() and 0x0FFF),
            title     = "🔴 Budget Exceeded — $category",
            body      = "Spent ${fmtLkr(spentLkr)} vs. ${fmtLkr(limitLkr)} budget — ${fmtLkr(over)} over limit.",
            priority  = NotificationCompat.PRIORITY_HIGH,
        )
        markPosted(ctx, key)
    }

    /**
     * Warning when a category hits 80% of its allocated budget.
     */
    fun postBudgetWarning(ctx: Context, category: String, pct: Int, limitLkr: Double) {
        val key = "budget_warning_$category"
        if (!cooldownPassed(ctx, key)) return
        post(
            ctx       = ctx,
            channelId = CHANNEL_ALERTS,
            notifId   = ID_BUDGET_WARNING_BASE + (category.hashCode() and 0x0FFF),
            title     = "⚠️ Budget Warning — $category",
            body      = "You've used $pct% of your ${fmtLkr(limitLkr)} $category budget. Slow down to stay on track.",
            priority  = NotificationCompat.PRIORITY_DEFAULT,
        )
        markPosted(ctx, key)
    }

    /**
     * Alert when the savings goal deadline is within [daysLeft] days.
     */
    fun postGoalDeadline(
        ctx: Context,
        goalName: String,
        daysLeft: Int,
        savedLkr: Double,
        targetLkr: Double,
    ) {
        val key = "goal_deadline"
        if (!cooldownPassed(ctx, key)) return
        val achieved = savedLkr >= targetLkr
        post(
            ctx       = ctx,
            channelId = CHANNEL_MILESTONES,
            notifId   = ID_GOAL_DEADLINE,
            title     = "⏰ Goal Deadline in $daysLeft Day${if (daysLeft == 1) "" else "s"}!",
            body      = "\"$goalName\" — ${fmtLkr(savedLkr)} of ${fmtLkr(targetLkr)} saved." +
                        if (achieved) " You've made it! 🎉" else " Keep pushing!",
            priority  = NotificationCompat.PRIORITY_DEFAULT,
        )
        markPosted(ctx, key)
    }

    /**
     * Celebration when goal progress crosses 25 / 50 / 75 / 100%.
     * @param milestone one of 25, 50, 75, 100
     */
    fun postGoalMilestone(
        ctx: Context,
        goalName: String,
        milestone: Int,
        savedLkr: Double,
        targetLkr: Double,
    ) {
        val key = "goal_milestone_$milestone"
        if (!cooldownPassed(ctx, key)) return
        val emoji = when (milestone) { 100 -> "🏆"; 75 -> "🥇"; 50 -> "🎯"; else -> "🌟" }
        val tail  = if (milestone >= 100) "Goal achieved! Incredible work." else "Keep the momentum going!"
        post(
            ctx       = ctx,
            channelId = CHANNEL_MILESTONES,
            notifId   = ID_GOAL_MILESTONE,
            title     = "$emoji $milestone% of \"$goalName\" Reached!",
            body      = "You've saved ${fmtLkr(savedLkr)} out of ${fmtLkr(targetLkr)}. $tail",
            priority  = NotificationCompat.PRIORITY_DEFAULT,
        )
        markPosted(ctx, key)
    }

    /**
     * Daily 8 PM summary of today's spending vs estimated daily budget.
     * No cooldown — AlarmManager already limits this to once per day.
     */
    fun postDailyDigest(ctx: Context, todaySpentLkr: Double, dailyBudgetLkr: Double, txCount: Int) {
        val statusLine = when {
            dailyBudgetLkr <= 0 -> "$txCount transaction${if (txCount == 1) "" else "s"} logged today."
            todaySpentLkr <= dailyBudgetLkr ->
                "${fmtLkr(dailyBudgetLkr - todaySpentLkr)} still available in today's budget."
            else ->
                "${fmtLkr(todaySpentLkr - dailyBudgetLkr)} over your daily budget — review spending."
        }
        post(
            ctx       = ctx,
            channelId = CHANNEL_DAILY,
            notifId   = ID_DAILY_DIGEST,
            title     = "📊 Today: ${fmtLkr(todaySpentLkr)} spent",
            body      = statusLine,
            priority  = NotificationCompat.PRIORITY_LOW,
        )
    }

    /**
     * Celebration when user hits their savings target for ≥ 3 consecutive months.
     */
    fun postSavingsStreak(ctx: Context, streak: Int) {
        val key = "savings_streak_$streak"
        if (!cooldownPassed(ctx, key)) return
        post(
            ctx       = ctx,
            channelId = CHANNEL_MILESTONES,
            notifId   = ID_SAVINGS_STREAK,
            title     = "🔥 $streak-Month Savings Streak!",
            body      = "You've hit your monthly savings target for $streak months straight. Outstanding discipline!",
            priority  = NotificationCompat.PRIORITY_DEFAULT,
        )
        markPosted(ctx, key)
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    private fun post(
        ctx: Context,
        channelId: String,
        notifId: Int,
        title: String,
        body: String,
        priority: Int,
    ) {
        val tapIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            ctx,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notifId, notif)
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun cooldownPassed(ctx: Context, key: String): Boolean =
        System.currentTimeMillis() - prefs(ctx).getLong(key, 0L) > COOLDOWN_MS

    private fun markPosted(ctx: Context, key: String) =
        prefs(ctx).edit().putLong(key, System.currentTimeMillis()).apply()

    /** Format as "LKR 1,234" */
    private fun fmtLkr(amount: Double): String =
        "LKR %,.0f".format(amount)
}
