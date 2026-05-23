package com.example.financeapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.BudgetCategorySummary
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.SavingsDeposit
import com.example.financeapp.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GoalStatus { ON_TRACK, AHEAD, BEHIND }

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh(monthYear: String = currentMonthYear()) {
        viewModelScope.launch {
            val goal = repository.getLatestGoal()
            val budgets = repository.getBudgetCategoriesForMonth(monthYear)
            val actuals = repository.getActualSpentByCategory(monthYear)
            val monthlyIncomeAvg = repository.getMonthlyIncomeAverage(3)

            // Merge: budgeted categories + any expense category that has spending this month
            val budgetMap = budgets.associateBy { it.categoryName }
            val allCategoryNames = (budgetMap.keys + actuals.keys).filter { it.isNotBlank() }.toSet()
            val summaries = allCategoryNames
                .map { name ->
                    BudgetCategorySummary(
                        categoryName = name,
                        allocatedAmount = budgetMap[name]?.allocatedAmount ?: 0.0,
                        actualSpent = actuals[name] ?: 0.0
                    )
                }
                .sortedWith(
                    compareByDescending<BudgetCategorySummary> { it.allocatedAmount > 0 }
                        .thenByDescending { it.actualSpent }
                )

            if (goal == null) {
                _state.value = BudgetUiState(
                    categoryBudgets = summaries,
                    monthlyIncomeAverage = monthlyIncomeAvg
                )
                return@launch
            }

            val monthsRemaining = calculateMonthsRemaining(goal.deadlineTimestamp)
            val requiredMonthly = calculateRequiredMonthlySavings(goal, monthsRemaining)
            val streak = computeSavingsStreak(goal.id, requiredMonthly)

            _state.value = BudgetUiState(
                activeGoal = goal,
                requiredMonthlySavings = requiredMonthly,
                progressPercent = calculateProgressPercent(goal),
                categoryBudgets = summaries,
                goalStatus = calculateGoalStatus(goal),
                daysRemaining = calculateDaysRemaining(goal.deadlineTimestamp),
                projectedCompletionDate = calculateProjectedCompletion(goal, requiredMonthly),
                savingsStreak = streak,
                monthlyIncomeAverage = monthlyIncomeAvg
            )
        }
    }

    fun createGoal(name: String, targetAmount: Double, currency: String, deadlineTimestamp: Long) {
        if (name.isBlank() || targetAmount <= 0.0 || deadlineTimestamp <= 0L) return
        viewModelScope.launch {
            val lkrAmount = if (currency == "USD") targetAmount * 300.0 else targetAmount
            val goal = Goal(
                id = "goal_${System.currentTimeMillis()}",
                name = name.trim(),
                targetAmount = lkrAmount,
                currentSavings = 0.0,
                currency = currency,
                deadlineTimestamp = deadlineTimestamp,
                createdAt = Instant.now().toEpochMilli()
            )
            repository.upsertGoal(goal)
            refresh()
        }
    }

    fun updateGoal(name: String, targetAmount: Double, currency: String, deadlineTimestamp: Long) {
        val existing = _state.value.activeGoal ?: return
        if (name.isBlank() || targetAmount <= 0.0 || deadlineTimestamp <= 0L) return
        viewModelScope.launch {
            val lkrAmount = if (currency == "USD") targetAmount * 300.0 else targetAmount
            repository.upsertGoal(
                existing.copy(
                    name = name.trim(),
                    targetAmount = lkrAmount,
                    currency = currency,
                    deadlineTimestamp = deadlineTimestamp
                )
            )
            refresh()
        }
    }

    fun addSavingsToGoal(amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val currentGoal = _state.value.activeGoal
                ?: repository.getLatestGoal()
                ?: return@launch

            val deposit = SavingsDeposit(
                id = UUID.randomUUID().toString(),
                goalId = currentGoal.id,
                amount = amount,
                timestamp = Instant.now().toEpochMilli()
            )
            repository.insertDeposit(deposit)
            repository.updateGoalSavings(currentGoal.id, currentGoal.currentSavings + amount)
            refresh()
        }
    }

    fun setCategoryBudget(category: String, amount: Double, monthYear: String = currentMonthYear()) {
        if (category.isBlank() || amount < 0.0) return
        viewModelScope.launch {
            repository.upsertBudgetCategory(
                BudgetCategory(
                    id = budgetCategoryId(category, monthYear),
                    categoryName = category,
                    allocatedAmount = amount,
                    monthYear = monthYear
                )
            )
            refresh(monthYear)
        }
    }

    // --- Pure calculation functions (internal for testing) ---

    fun calculateRequiredMonthlySavings(goal: Goal, monthsRemaining: Int): Double {
        if (monthsRemaining <= 0) return 0.0
        val remaining = (goal.targetAmount - goal.currentSavings).coerceAtLeast(0.0)
        return remaining / monthsRemaining
    }

    fun calculateProgressPercent(goal: Goal): Double {
        if (goal.targetAmount <= 0.0) return 0.0
        return (goal.currentSavings / goal.targetAmount) * 100.0
    }

    fun calculateGoalStatus(goal: Goal): GoalStatus {
        val now = Instant.now().toEpochMilli()
        val totalMs = goal.deadlineTimestamp - goal.createdAt
        if (totalMs <= 0L) return GoalStatus.ON_TRACK
        val elapsedMs = (now - goal.createdAt).coerceAtLeast(0L)
        val elapsedFraction = (elapsedMs.toDouble() / totalMs).coerceIn(0.0, 1.0)
        if (elapsedFraction < 0.01) return GoalStatus.ON_TRACK
        val expectedSavings = elapsedFraction * goal.targetAmount
        return when {
            goal.currentSavings >= expectedSavings * 1.1 -> GoalStatus.AHEAD
            goal.currentSavings >= expectedSavings * 0.9 -> GoalStatus.ON_TRACK
            else -> GoalStatus.BEHIND
        }
    }

    fun calculateDaysRemaining(deadlineTimestamp: Long): Int {
        if (deadlineTimestamp <= 0L) return 0
        val diffMs = deadlineTimestamp - Instant.now().toEpochMilli()
        return (diffMs / DAY_MILLIS).toInt().coerceAtLeast(0)
    }

    fun calculateProjectedCompletion(goal: Goal, requiredMonthlySavings: Double): Long {
        val remaining = (goal.targetAmount - goal.currentSavings).coerceAtLeast(0.0)
        if (remaining <= 0.0) return Instant.now().toEpochMilli()
        if (requiredMonthlySavings <= 0.0) return 0L
        val monthsToComplete = remaining / requiredMonthlySavings
        val millisToComplete = (monthsToComplete * 30.44 * DAY_MILLIS).toLong()
        return Instant.now().toEpochMilli() + millisToComplete
    }

    fun calculateMonthsEarlier(totalReduction: Double, goal: Goal): Int {
        val monthsRemaining = calculateMonthsRemaining(goal.deadlineTimestamp)
        if (monthsRemaining <= 0) return 0
        val remaining = (goal.targetAmount - goal.currentSavings).coerceAtLeast(0.0)
        val requiredRate = calculateRequiredMonthlySavings(goal, monthsRemaining)
        if (requiredRate <= 0.0 || totalReduction <= 0.0) return 0
        val newRate = requiredRate + totalReduction
        val newMonths = ceil(remaining / newRate).toInt()
        return (monthsRemaining - newMonths).coerceAtLeast(0)
    }

    fun calculateMonthsRemaining(deadlineTimestamp: Long): Int {
        if (deadlineTimestamp <= 0L) return 0
        val zone = ZoneId.systemDefault()
        val today = YearMonth.from(Instant.now().atZone(zone))
        val deadline = YearMonth.from(Instant.ofEpochMilli(deadlineTimestamp).atZone(zone))
        val months = (deadline.year - today.year) * 12 + (deadline.monthValue - today.monthValue)
        return months.coerceAtLeast(0)
    }

    fun computeSavingsStreakFromDeposits(
        deposits: List<SavingsDeposit>,
        requiredMonthly: Double
    ): Int {
        if (requiredMonthly <= 0.0 || deposits.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        var streak = 0
        var checkMonth = YearMonth.now()
        repeat(24) {
            val start = checkMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = checkMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val monthTotal = deposits.filter { it.timestamp in start..end }.sumOf { it.amount }
            if (monthTotal >= requiredMonthly) {
                streak++
                checkMonth = checkMonth.minusMonths(1)
            } else {
                return streak
            }
        }
        return streak
    }

    private suspend fun computeSavingsStreak(goalId: String, requiredMonthly: Double): Int =
        computeSavingsStreakFromDeposits(repository.getDepositsForGoal(goalId), requiredMonthly)

    private fun currentMonthYear(): String =
        YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    private fun budgetCategoryId(category: String, monthYear: String): String =
        "${monthYear}_${category.lowercase(Locale.US).replace(" ", "_")}"

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}

data class BudgetUiState(
    val activeGoal: Goal? = null,
    val requiredMonthlySavings: Double = 0.0,
    val progressPercent: Double = 0.0,
    val categoryBudgets: List<BudgetCategorySummary> = emptyList(),
    val goalStatus: GoalStatus = GoalStatus.ON_TRACK,
    val daysRemaining: Int = 0,
    val projectedCompletionDate: Long = 0L,
    val savingsStreak: Int = 0,
    val monthlyIncomeAverage: Double = 0.0
)
