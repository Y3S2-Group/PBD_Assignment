package com.example.financeapp.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.financeapp.domain.repository.BudgetRepository
import com.example.financeapp.domain.repository.ExpenseRepository
import com.example.financeapp.domain.repository.IncomeRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hilt-injected Service that runs smart financial push-notification checks.
 *
 * Two modes, selected by the action on the incoming Intent:
 *
 * [ACTION_CHECK_ALERTS] (default) — triggered by ViewModels after any data write.
 *   Evaluates 7 real-time conditions and fires OS notifications via
 *   [VaultNotificationHelper] when thresholds are crossed.
 *
 * [ACTION_DAILY_DIGEST] — triggered by AlarmManager at 8 PM every day.
 *   Computes today's total spend vs. estimated daily budget and posts a
 *   low-priority summary notification.
 *
 * The service stops itself once the coroutine finishes (START_NOT_STICKY).
 */
@AndroidEntryPoint
class FinancialAlertsService : Service() {

    @Inject lateinit var incomeRepo: IncomeRepository
    @Inject lateinit var expenseRepo: ExpenseRepository
    @Inject lateinit var budgetRepo: BudgetRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DAILY_DIGEST -> runDailyDigest(startId)
            else                -> runAlertChecks(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Real-time alert checks ──────────────────────────────────────────────────

    private fun runAlertChecks(startId: Int) {
        serviceScope.launch {
            runCatching {
                val ctx = applicationContext
                val zone = ZoneId.systemDefault()
                val currentMonth = YearMonth.now()
                val monthYear = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val monthEnd = currentMonth.plusMonths(1).atDay(1)
                    .atStartOfDay(zone).toInstant().toEpochMilli() - 1

                val incomeThisMonth  = incomeRepo.sumAmountLkrBetween(monthStart, monthEnd)
                val expenseThisMonth = expenseRepo.sumAmountLkrBetween(monthStart, monthEnd)

                // 1. Expense overshoot — spending exceeds income this month
                if (incomeThisMonth > 0 && expenseThisMonth > incomeThisMonth) {
                    val pct = (expenseThisMonth / incomeThisMonth * 100).toInt()
                    VaultNotificationHelper.postOverspendAlert(ctx, pct)
                }

                // 2 & 3. Per-category budget alerts
                val budgetCategories = budgetRepo.getBudgetCategoriesForMonth(monthYear)
                val actualSpent = budgetRepo.getActualSpentByCategory(monthYear)
                budgetCategories.forEach { cat ->
                    val spent = actualSpent[cat.categoryName] ?: 0.0
                    val limit = cat.allocatedAmount
                    if (limit > 0) {
                        val pct = (spent / limit * 100).toInt()
                        when {
                            pct >= 100 -> VaultNotificationHelper.postBudgetExceeded(
                                ctx, cat.categoryName, spent, limit
                            )
                            pct >= 80 -> VaultNotificationHelper.postBudgetWarning(
                                ctx, cat.categoryName, pct, limit
                            )
                        }
                    }
                }

                // 4. Large single expense (≥ 15% of monthly income)
                if (incomeThisMonth > 0) {
                    val threshold = incomeThisMonth * 0.15
                    expenseRepo.getAllExpenses()
                        .maxByOrNull { it.timestamp }  // most-recently added expense
                        ?.let { latest ->
                            if (latest.amountLkr >= threshold) {
                                val pct = (latest.amountLkr / incomeThisMonth * 100).toInt()
                                VaultNotificationHelper.postLargeExpense(
                                    ctx, latest.category, latest.amountLkr, pct
                                )
                            }
                        }
                }

                // 5 & 6. Savings-goal alerts
                budgetRepo.getLatestGoal()?.let { goal ->
                    val now = System.currentTimeMillis()
                    val daysLeft = ((goal.deadlineTimestamp - now) / DAY_MS).toInt()

                    // Deadline within 7 days
                    if (daysLeft in 1..7) {
                        VaultNotificationHelper.postGoalDeadline(
                            ctx, goal.name, daysLeft, goal.currentSavings, goal.targetAmount
                        )
                    }

                    // Milestone crosses
                    if (goal.targetAmount > 0) {
                        val pct = (goal.currentSavings / goal.targetAmount * 100).toInt()
                        listOf(100, 75, 50, 25).firstOrNull { pct >= it }?.let { ms ->
                            VaultNotificationHelper.postGoalMilestone(
                                ctx, goal.name, ms, goal.currentSavings, goal.targetAmount
                            )
                        }
                    }
                }

                // 7. Savings streak (≥ 3 consecutive months meeting target)
                checkSavingsStreak(ctx)
            }
            stopSelf(startId)
        }
    }

    // ── Daily digest ────────────────────────────────────────────────────────────

    private fun runDailyDigest(startId: Int) {
        serviceScope.launch {
            runCatching {
                val ctx = applicationContext
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val currentMonth = YearMonth.now()

                val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val todayEnd   = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val monthEnd   = currentMonth.plusMonths(1).atDay(1)
                    .atStartOfDay(zone).toInstant().toEpochMilli() - 1

                val todaySpent      = expenseRepo.sumAmountLkrBetween(todayStart, todayEnd)
                val incomeThisMonth = incomeRepo.sumAmountLkrBetween(monthStart, monthEnd)
                val dailyBudget     = if (incomeThisMonth > 0)
                    incomeThisMonth / currentMonth.lengthOfMonth() else 0.0

                val txCount = expenseRepo.getAllExpenses().count { it.timestamp in todayStart..todayEnd }

                VaultNotificationHelper.postDailyDigest(ctx, todaySpent, dailyBudget, txCount)
            }
            stopSelf(startId)
        }
    }

    // ── Savings streak helper ───────────────────────────────────────────────────

    private suspend fun checkSavingsStreak(ctx: Context) {
        val goal = budgetRepo.getLatestGoal() ?: return
        if (goal.targetAmount <= 0) return

        val totalMonths = ((goal.deadlineTimestamp - goal.createdAt) /
                (30.44 * DAY_MS)).coerceAtLeast(1.0)
        val requiredMonthly = goal.targetAmount / totalMonths

        val zone = ZoneId.systemDefault()
        var streak = 0
        var checkMonth = YearMonth.now()
        repeat(12) {
            val start = checkMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = checkMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val netSaved = incomeRepo.sumAmountLkrBetween(start, end) -
                    expenseRepo.sumAmountLkrBetween(start, end)
            if (netSaved >= requiredMonthly) {
                streak++
                checkMonth = checkMonth.minusMonths(1)
            } else return@repeat
        }
        if (streak >= 3) VaultNotificationHelper.postSavingsStreak(ctx, streak)
    }

    companion object {
        const val ACTION_CHECK_ALERTS = "com.example.financeapp.ACTION_CHECK_ALERTS"
        const val ACTION_DAILY_DIGEST = "com.example.financeapp.ACTION_DAILY_DIGEST"
        private const val DAY_MS = 24L * 60 * 60 * 1_000

        /** Start the service in alert-check mode from any Context (e.g. a ViewModel). */
        fun startAlertCheck(ctx: Context) {
            ctx.startService(Intent(ctx, FinancialAlertsService::class.java).apply {
                action = ACTION_CHECK_ALERTS
            })
        }
    }
}
