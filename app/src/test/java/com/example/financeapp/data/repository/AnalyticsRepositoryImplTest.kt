package com.example.financeapp.data.repository

import com.example.financeapp.data.local.AnalyticsDao
import com.example.financeapp.domain.model.AnalyticsSummaryCache
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.repository.ExpenseRepository
import com.example.financeapp.domain.repository.IncomeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsRepositoryImplTest {
    private val marchNow = 1_711_584_000_000L // 2024-03-28T00:00:00Z

    private class FakeAnalyticsDao : AnalyticsDao {
        private var latest: AnalyticsSummaryCache? = null

        override suspend fun insert(summary: AnalyticsSummaryCache) {
            latest = summary
        }

        override fun getLatest(): AnalyticsSummaryCache? = latest

        override fun getByMonthKey(monthKey: String): AnalyticsSummaryCache? =
            latest?.takeIf { it.monthKey == monthKey }
    }

    private class FakeIncomeRepository(
        seed: List<Income>
    ) : IncomeRepository {
        private val entries = seed.toMutableList()

        override suspend fun insertIncome(income: Income) {
            entries.add(income)
        }

        override suspend fun getAllIncomes(): List<Income> = entries.toList()

        override suspend fun getBySourceType(sourceType: String): List<Income> =
            entries.filter { it.sourceType == sourceType }

        override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
            entries.filter { it.date in startInclusive..endInclusive }.sumOf { it.amountLKR }
    }

    private class FakeExpenseRepository(
        seed: List<Expense>
    ) : ExpenseRepository {
        private val entries = seed.toMutableList()

        override suspend fun insertExpense(expense: Expense) {
            entries.add(expense)
        }

        override suspend fun getAllExpenses(): List<Expense> = entries.toList()

        override suspend fun getBySpendingType(spendingType: String): List<Expense> =
            entries.filter { it.spendingType == spendingType }

        override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
            entries.filter { it.timestamp in startInclusive..endInclusive }.sumOf { it.amountLkr }
    }

    @Test
    fun buildDashboardSummary_computesCurrentMonthMetrics() = runTest {
        val analyticsDao = FakeAnalyticsDao()
        val repository = AnalyticsRepositoryImpl(
            analyticsDao = analyticsDao,
            incomeRepository = FakeIncomeRepository(
                listOf(
                    Income("inc_1", 3000.0, "USD", 900_000.0, "SALARY", 1_710_720_000_000L),
                    Income("inc_2", 500.0, "USD", 150_000.0, "FREELANCE", 1_711_324_800_000L),
                    Income("inc_old", 250.0, "USD", 75_000.0, "ADSENSE", 1_709_942_400_000L)
                )
            ),
            expenseRepository = FakeExpenseRepository(
                listOf(
                    Expense("exp_1", 120_000.0, "Food", "DISCRETIONARY", "Card", 1_710_806_400_000L),
                    Expense("exp_2", 80_000.0, "Rent", "COMMITTED", "Transfer", 1_711_411_200_000L),
                    Expense("exp_3", 50_000.0, "Food", "DISCRETIONARY", "Cash", 1_711_497_600_000L),
                    Expense("exp_old", 40_000.0, "Travel", "DISCRETIONARY", "Card", 1_709_856_000_000L)
                )
            )
        )

        val summary = repository.buildDashboardSummary(nowMillis = marchNow)

        assertEquals(1_125_000.0, summary.totalIncomeLkr, 0.01)
        assertEquals(290_000.0, summary.totalExpenseLkr, 0.01)
        assertEquals(835_000.0, summary.netSavingsLkr, 0.01)
        assertEquals(74.22, summary.savingsRate, 0.01)
        assertEquals(3, summary.categoryBreakdown.size)
        assertEquals("Food", summary.categoryBreakdown.first().label)
        assertEquals(170_000.0, summary.categoryBreakdown.first().amountLkr, 0.01)
        assertEquals(72.41, summary.spendingBreakdown.discretionaryPercentage, 0.01)
        assertEquals(27.59, summary.spendingBreakdown.committedPercentage, 0.01)
        assertTrue(summary.healthScore in 0..100)
        assertTrue(summary.insights.isNotEmpty())
    }

    @Test
    fun buildDashboardSummary_returnsEmptyStateWhenNoDataExists() = runTest {
        val repository = AnalyticsRepositoryImpl(
            analyticsDao = FakeAnalyticsDao(),
            incomeRepository = FakeIncomeRepository(emptyList()),
            expenseRepository = FakeExpenseRepository(emptyList())
        )

        val summary = repository.buildDashboardSummary(nowMillis = marchNow)

        assertEquals(0.0, summary.totalIncomeLkr, 0.0)
        assertEquals(0.0, summary.totalExpenseLkr, 0.0)
        assertEquals(0.0, summary.netSavingsLkr, 0.0)
        assertTrue(summary.categoryBreakdown.isEmpty())
        assertTrue(summary.incomeBreakdown.isEmpty())
        assertEquals("Set your monthly goal", summary.goal.title)
    }

    @Test
    fun refreshDashboardSummary_persistsSummaryToCache() = runTest {
        val analyticsDao = FakeAnalyticsDao()
        val repository = AnalyticsRepositoryImpl(
            analyticsDao = analyticsDao,
            incomeRepository = FakeIncomeRepository(
                listOf(Income("inc_1", 3000.0, "USD", 900_000.0, "SALARY", 1_710_720_000_000L))
            ),
            expenseRepository = FakeExpenseRepository(
                listOf(Expense("exp_1", 120_000.0, "Food", "DISCRETIONARY", "Card", 1_710_806_400_000L))
            )
        )

        repository.refreshDashboardSummary(marchNow)

        val cached = repository.getCachedDashboardSummary()
        assertEquals(900_000.0, cached?.totalIncomeLkr ?: 0.0, 0.01)
        assertEquals(120_000.0, cached?.totalExpenseLkr ?: 0.0, 0.01)
        assertTrue(cached?.insights?.isNotEmpty() == true)
        assertTrue(cached?.monthlyTrend?.isNotEmpty() == true)
        assertTrue(cached?.categoryBreakdown?.isNotEmpty() == true)
    }
}
