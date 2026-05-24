package com.example.financeapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.sync.FirestoreSyncService
import com.example.financeapp.domain.repository.BudgetRepository
import com.example.financeapp.domain.repository.ExpenseRepository
import com.example.financeapp.domain.repository.IncomeRepository
import com.example.financeapp.util.AppEventBus
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MonthlyTrendPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

data class CategorySpendItem(
    val name: String,
    val amount: Double,
    val percent: Double
)

data class DashboardUiState(
    val incomeThisMonth: Double = 0.0,
    val expenseThisMonth: Double = 0.0,
    val netSavingsThisMonth: Double = 0.0,
    val goalName: String = "",
    val goalProgressPercent: Double = 0.0,
    val goalCurrentSavings: Double = 0.0,
    val goalTargetAmount: Double = 0.0,
    val goalDaysRemaining: Int = 0,
    val hasGoal: Boolean = false,
    val categorySpend: Map<String, Double> = emptyMap(),
    val committedSpend: Double = 0.0,
    val discretionarySpend: Double = 0.0,
    val monthlyTrend: List<MonthlyTrendPoint> = emptyList(),
    val topCategories: List<CategorySpendItem> = emptyList(),
    val incomeBySource: Map<String, Double> = emptyMap(),
    val healthScore: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val incomeRepo: IncomeRepository,
    private val expenseRepo: ExpenseRepository,
    private val budgetRepo: BudgetRepository,
    private val eventBus: AppEventBus,
    private val syncService: FirestoreSyncService,
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Pull Firestore data into Room so the UI is up-to-date across devices.
            firebaseAuth.currentUser?.uid?.let { syncService.syncAll(it) }
        }
        refresh()
        // Re-run refresh whenever income, expenses, or budget/goal data changes in any screen.
        viewModelScope.launch {
            eventBus.events.collect { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val currentMonth = YearMonth.now()
            val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            // FR1: at-a-glance numbers
            val incomeThisMonth = incomeRepo.sumAmountLkrBetween(monthStart, monthEnd)
            val expenseThisMonth = expenseRepo.sumAmountLkrBetween(monthStart, monthEnd)
            val netSavings = incomeThisMonth - expenseThisMonth

            // FR2: category spend for donut chart
            val categorySpend = expenseRepo.sumAmountLkrByCategoryBetween(monthStart, monthEnd)

            // FR3: committed vs discretionary this month
            val committedSpend = expenseRepo.sumAmountLkrBySpendingTypeBetween("COMMITTED", monthStart, monthEnd)
            val discretionarySpend = expenseRepo.sumAmountLkrBySpendingTypeBetween("DISCRETIONARY", monthStart, monthEnd)

            // FR4: 6-month income/expense trend
            val monthlyTrend = (5 downTo 0).map { offset ->
                val month = currentMonth.minusMonths(offset.toLong())
                val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                val label = month.format(DateTimeFormatter.ofPattern("MMM", Locale.US))
                val inc = incomeRepo.sumAmountLkrBetween(start, end)
                val exp = expenseRepo.sumAmountLkrBetween(start, end)
                MonthlyTrendPoint(label, inc, exp)
            }

            // FR5: top spending categories (max 5)
            val totalCategoryExpense = categorySpend.values.sum().coerceAtLeast(1.0)
            val topCategories = categorySpend.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (name, amount) ->
                    CategorySpendItem(name, amount, (amount / totalCategoryExpense) * 100.0)
                }

            // FR6: income by source — this month first, fall back to all-time
            val allIncomes = incomeRepo.getAllIncomes()
            val thisMonthIncomes = allIncomes.filter { it.date in monthStart..monthEnd }
            val sourceMap = (thisMonthIncomes.ifEmpty { allIncomes })
                .groupBy { it.sourceType.ifBlank { "Other" } }
                .mapValues { (_, list) -> list.sumOf { it.amountLKR } }
                .filter { it.value > 0 }

            // Goal data
            val goal = budgetRepo.getLatestGoal()
            val goalProgressPercent = if (goal != null && goal.targetAmount > 0)
                (goal.currentSavings / goal.targetAmount) * 100.0 else 0.0
            val goalDaysRemaining = if (goal != null && goal.deadlineTimestamp > 0) {
                ((goal.deadlineTimestamp - Instant.now().toEpochMilli()) / DAY_MILLIS)
                    .toInt().coerceAtLeast(0)
            } else 0

            // FR7: financial health score
            val healthScore = computeHealthScore(incomeThisMonth, expenseThisMonth, goalProgressPercent)

            _state.value = DashboardUiState(
                incomeThisMonth = incomeThisMonth,
                expenseThisMonth = expenseThisMonth,
                netSavingsThisMonth = netSavings,
                goalName = goal?.name ?: "",
                goalProgressPercent = goalProgressPercent,
                goalCurrentSavings = goal?.currentSavings ?: 0.0,
                goalTargetAmount = goal?.targetAmount ?: 0.0,
                goalDaysRemaining = goalDaysRemaining,
                hasGoal = goal != null,
                categorySpend = categorySpend,
                committedSpend = committedSpend,
                discretionarySpend = discretionarySpend,
                monthlyTrend = monthlyTrend,
                topCategories = topCategories,
                incomeBySource = sourceMap,
                healthScore = healthScore,
                isLoading = false
            )
        }
    }

    // FR7: score = savings rate (0-50) + expense control (0-20) + goal progress (0-30)
    internal fun computeHealthScore(
        income: Double,
        expense: Double,
        goalProgressPercent: Double
    ): Int {
        val savingsPts = if (income > 0) {
            val savingsRate = ((income - expense) / income).coerceIn(0.0, 1.0)
            // 20 % savings rate → full 50 pts
            (savingsRate * 250.0).coerceAtMost(50.0)
        } else 0.0

        val expensePts = if (income > 0) {
            val ratio = (expense / income).coerceIn(0.0, 2.0)
            when {
                ratio < 0.70 -> 20.0
                ratio < 0.80 -> 16.0
                ratio < 0.90 -> 10.0
                ratio < 1.00 -> 5.0
                else -> 0.0
            }
        } else 0.0

        val goalPts = (goalProgressPercent / 100.0 * 30.0).coerceAtMost(30.0)

        return (savingsPts + expensePts + goalPts).toInt().coerceIn(0, 100)
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
