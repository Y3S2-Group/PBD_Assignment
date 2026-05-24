package com.example.financeapp.data.repository

import com.example.financeapp.data.local.AnalyticsDao
import com.example.financeapp.domain.model.AnalyticsSummaryCache
import com.example.financeapp.domain.model.BreakdownSlice
import com.example.financeapp.domain.model.DashboardAnalytics
import com.example.financeapp.domain.model.DashboardInsight
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.model.GoalSnapshot
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.model.InsightTone
import com.example.financeapp.domain.model.SpendingBreakdown
import com.example.financeapp.domain.model.TrendPoint
import com.example.financeapp.domain.repository.AnalyticsRepository
import com.example.financeapp.domain.repository.ExpenseRepository
import com.example.financeapp.domain.repository.IncomeRepository
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository
) : AnalyticsRepository {
    override suspend fun buildDashboardSummary(nowMillis: Long): DashboardAnalytics =
        withContext(Dispatchers.Default) {
            val incomes = incomeRepository.getAllIncomes()
            val expenses = expenseRepository.getAllExpenses()

            if (incomes.isEmpty() && expenses.isEmpty()) {
                return@withContext DashboardAnalytics.empty().copy(
                    insights = listOf(
                        DashboardInsight(
                            title = "Start tracking",
                            message = "Add income and expense entries to unlock dashboard analytics.",
                            tone = InsightTone.Neutral
                        )
                    )
                )
            }

            val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
            val (monthStart, monthEnd) = monthBounds(now)

            val currentIncomes = incomes.filter { it.date in monthStart..monthEnd }
            val currentExpenses = expenses.filter { it.timestamp in monthStart..monthEnd }

            val totalIncome = currentIncomes.sumOf { it.amountLKR }
            val totalExpenses = currentExpenses.sumOf { it.amountLkr }
            val netSavings = totalIncome - totalExpenses
            val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) * 100 else 0.0

            val categoryBreakdown = buildBreakdown(
                items = currentExpenses,
                total = totalExpenses,
                keySelector = { it.category.ifBlank { "Other" } },
                amountSelector = { it.amountLkr }
            )

            val incomeBreakdown = buildBreakdown(
                items = currentIncomes,
                total = totalIncome,
                keySelector = { it.sourceType.ifBlank { "Other" } },
                amountSelector = { it.amountLKR }
            )

            val spendingBreakdown = buildSpendingBreakdown(currentExpenses, totalExpenses)
            val monthlyTrend = buildTrend(incomes, expenses, now)
            val goal = buildGoalSnapshot(totalExpenses, netSavings)
            val insights = buildInsights(
                totalIncome = totalIncome,
                netSavings = netSavings,
                categoryBreakdown = categoryBreakdown,
                spendingBreakdown = spendingBreakdown,
                incomeBreakdown = incomeBreakdown
            )

            DashboardAnalytics(
                totalIncomeLkr = totalIncome,
                totalExpenseLkr = totalExpenses,
                netSavingsLkr = netSavings,
                savingsRate = savingsRate.rounded(),
                healthScore = computeHealthScore(savingsRate, spendingBreakdown, netSavings),
                goal = goal,
                categoryBreakdown = categoryBreakdown,
                incomeBreakdown = incomeBreakdown,
                spendingBreakdown = spendingBreakdown,
                monthlyTrend = monthlyTrend,
                insights = insights
            )
        }

    override suspend fun refreshDashboardSummary(nowMillis: Long): DashboardAnalytics {
        val summary = buildDashboardSummary(nowMillis)
        withContext(Dispatchers.IO) {
            analyticsDao.insert(
                AnalyticsSummaryCache.from(
                    summary = summary,
                    monthKey = monthKey(nowMillis),
                    updatedAt = nowMillis
                )
            )
        }
        return summary
    }

    override suspend fun getCachedDashboardSummary(): DashboardAnalytics? =
        withContext(Dispatchers.IO) {
            analyticsDao.getLatest()?.toDashboardAnalytics()
        }

    private fun buildGoalSnapshot(totalExpenses: Double, netSavings: Double): GoalSnapshot {
        val currentAmount = netSavings.coerceAtLeast(0.0)
        val targetAmount = maxOf(totalExpenses * 3, 150_000.0)
        val progress = if (targetAmount > 0) (currentAmount / targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
        return GoalSnapshot(
            title = "Emergency Buffer",
            currentAmountLkr = currentAmount,
            targetAmountLkr = targetAmount,
            daysLeft = 90,
            progress = progress
        )
    }

    private fun buildInsights(
        totalIncome: Double,
        netSavings: Double,
        categoryBreakdown: List<BreakdownSlice>,
        spendingBreakdown: SpendingBreakdown,
        incomeBreakdown: List<BreakdownSlice>
    ): List<DashboardInsight> {
        val insights = mutableListOf<DashboardInsight>()

        if (netSavings > 0) {
            insights += DashboardInsight(
                title = "Positive cash flow",
                message = "You kept LKR ${netSavings.rounded()} after expenses this month.",
                tone = InsightTone.Positive
            )
        } else if (totalIncome > 0) {
            insights += DashboardInsight(
                title = "Spending above income",
                message = "Your expenses are exceeding this month's income. Review committed costs first.",
                tone = InsightTone.Warning
            )
        }

        categoryBreakdown.firstOrNull()?.let { topCategory ->
            insights += DashboardInsight(
                title = "Top spending category",
                message = "${topCategory.label} accounts for ${topCategory.percentage.rounded()}% of spending.",
                tone = if (topCategory.percentage >= 40) InsightTone.Warning else InsightTone.Neutral
            )
        }

        if (spendingBreakdown.discretionaryPercentage > spendingBreakdown.committedPercentage) {
            insights += DashboardInsight(
                title = "Discretionary spend is leading",
                message = "Optional spending is ${spendingBreakdown.discretionaryPercentage.rounded()}% of monthly expenses.",
                tone = InsightTone.Warning
            )
        } else if (spendingBreakdown.committedAmountLkr > 0 || spendingBreakdown.discretionaryAmountLkr > 0) {
            insights += DashboardInsight(
                title = "Fixed costs are under control",
                message = "Committed spending remains the larger share of your expense plan.",
                tone = InsightTone.Positive
            )
        }

        incomeBreakdown.firstOrNull()?.let { topSource ->
            insights += DashboardInsight(
                title = "Main income source",
                message = "${topSource.label} contributes ${topSource.percentage.rounded()}% of income.",
                tone = InsightTone.Neutral
            )
        }

        return insights.take(3)
    }

    private fun computeHealthScore(
        savingsRate: Double,
        spendingBreakdown: SpendingBreakdown,
        netSavings: Double
    ): Int {
        val savingsComponent = savingsRate.coerceIn(0.0, 50.0)
        val balanceComponent = if (netSavings > 0) 30.0 else 0.0
        val discretionaryPenalty = (spendingBreakdown.discretionaryPercentage - 50.0).coerceAtLeast(0.0) * 0.4
        return (savingsComponent + balanceComponent + 20.0 - discretionaryPenalty)
            .coerceIn(0.0, 100.0)
            .toInt()
    }

    private fun buildTrend(
        incomes: List<Income>,
        expenses: List<Expense>,
        now: ZonedDateTime
    ): List<TrendPoint> {
        return (5 downTo 0).map { offset ->
            val month = now.minusMonths(offset.toLong())
            val (start, end) = monthBounds(month)
            val incomeTotal = incomes.filter { it.date in start..end }.sumOf { it.amountLKR }
            val expenseTotal = expenses.filter { it.timestamp in start..end }.sumOf { it.amountLkr }
            TrendPoint(
                label = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                incomeLkr = incomeTotal,
                expenseLkr = expenseTotal,
                savingsLkr = incomeTotal - expenseTotal
            )
        }
    }

    private fun buildSpendingBreakdown(
        expenses: List<Expense>,
        totalExpenses: Double
    ): SpendingBreakdown {
        val committed = expenses
            .filter { it.spendingType.equals("COMMITTED", ignoreCase = true) }
            .sumOf { it.amountLkr }
        val discretionary = expenses
            .filter { it.spendingType.equals("DISCRETIONARY", ignoreCase = true) }
            .sumOf { it.amountLkr }
        return SpendingBreakdown(
            committedAmountLkr = committed,
            discretionaryAmountLkr = discretionary,
            committedPercentage = ratio(committed, totalExpenses),
            discretionaryPercentage = ratio(discretionary, totalExpenses)
        )
    }

    private fun <T> buildBreakdown(
        items: List<T>,
        total: Double,
        keySelector: (T) -> String,
        amountSelector: (T) -> Double
    ): List<BreakdownSlice> {
        if (items.isEmpty() || total <= 0) return emptyList()
        return items.groupBy(keySelector)
            .map { (label, group) ->
                val amount = group.sumOf(amountSelector)
                BreakdownSlice(
                    label = label,
                    amountLkr = amount,
                    percentage = ratio(amount, total)
                )
            }
            .sortedByDescending { it.amountLkr }
            .take(4)
    }

    private fun ratio(amount: Double, total: Double): Double {
        return if (total > 0) ((amount / total) * 100).rounded() else 0.0
    }

    private fun monthBounds(month: ZonedDateTime): Pair<Long, Long> {
        val start = month
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val end = start
            .plusMonths(1)
            .minusNanos(1)
        return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
    }

    private fun monthKey(nowMillis: Long): String {
        return Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .let { "${it.year}-${it.monthValue.toString().padStart(2, '0')}" }
    }

    private fun AnalyticsSummaryCache.toDashboardAnalytics(): DashboardAnalytics {
        val topCategory = topCategoryLabel?.let {
            BreakdownSlice(
                label = it,
                amountLkr = topCategoryAmountLkr,
                percentage = topCategoryPercentage
            )
        }
        val topIncomeSource = topIncomeSourceLabel?.let {
            BreakdownSlice(
                label = it,
                amountLkr = topIncomeSourceAmountLkr,
                percentage = topIncomeSourcePercentage
            )
        }
        val insight = primaryInsightTitle?.let { title ->
            DashboardInsight(
                title = title,
                message = primaryInsightMessage ?: "",
                tone = primaryInsightTone?.let { enumValueOf<InsightTone>(it) } ?: InsightTone.Neutral
            )
        }

        val categoryBreakdown = AnalyticsSummaryCache.decodeBreakdown(categoryBreakdownPayload)
            .ifEmpty { listOfNotNull(topCategory) }
        val incomeBreakdown = AnalyticsSummaryCache.decodeBreakdown(incomeBreakdownPayload)
            .ifEmpty { listOfNotNull(topIncomeSource) }
        val monthlyTrend = AnalyticsSummaryCache.decodeTrend(monthlyTrendPayload)
        val insights = AnalyticsSummaryCache.decodeInsights(insightsPayload)
            .ifEmpty { listOfNotNull(insight) }

        return DashboardAnalytics(
            totalIncomeLkr = totalIncomeLkr,
            totalExpenseLkr = totalExpenseLkr,
            netSavingsLkr = netSavingsLkr,
            savingsRate = savingsRate,
            healthScore = healthScore,
            goal = GoalSnapshot(
                title = goalTitle,
                currentAmountLkr = goalCurrentAmountLkr,
                targetAmountLkr = goalTargetAmountLkr,
                daysLeft = goalDaysLeft,
                progress = goalProgress
            ),
            categoryBreakdown = categoryBreakdown,
            incomeBreakdown = incomeBreakdown,
            spendingBreakdown = SpendingBreakdown(
                committedAmountLkr = committedAmountLkr,
                discretionaryAmountLkr = discretionaryAmountLkr,
                committedPercentage = committedPercentage,
                discretionaryPercentage = discretionaryPercentage
            ),
            monthlyTrend = monthlyTrend,
            insights = insights
        )
    }

    private fun Double.rounded(): Double = String.format(Locale.ENGLISH, "%.2f", this).toDouble()
}
