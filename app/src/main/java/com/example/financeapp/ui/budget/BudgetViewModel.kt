package com.example.financeapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.BudgetCategorySummary
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val goal = withContext(Dispatchers.IO) {
                repository.getLatestGoal()
            } ?: createDefaultGoal().also { defaultGoal ->
                withContext(Dispatchers.IO) {
                    repository.upsertGoal(defaultGoal)
                }
            }

            val budgets = withContext(Dispatchers.IO) {
                repository.getBudgetCategoriesForMonth(monthYear)
            }
            val actuals = withContext(Dispatchers.IO) {
                repository.getActualSpentByCategory(monthYear)
            }

            val summaries = budgets.map { category ->
                BudgetCategorySummary(
                    categoryName = category.categoryName,
                    allocatedAmount = category.allocatedAmount,
                    actualSpent = actuals[category.categoryName] ?: 0.0
                )
            }

            val monthsRemaining = calculateMonthsRemaining(goal.deadlineTimestamp)
            _state.value = BudgetUiState(
                activeGoal = goal,
                requiredMonthlySavings = calculateRequiredMonthlySavings(goal, monthsRemaining),
                progressPercent = calculateProgressPercent(goal),
                categoryBudgets = summaries
            )
        }
    }

    fun calculateRequiredMonthlySavings(goal: Goal, monthsRemaining: Int): Double {
        if (monthsRemaining <= 0) return 0.0
        val remaining = (goal.targetAmount - goal.currentSavings).coerceAtLeast(0.0)
        return remaining / monthsRemaining
    }

    fun calculateProgressPercent(goal: Goal): Double {
        if (goal.targetAmount <= 0.0) return 0.0
        return (goal.currentSavings / goal.targetAmount) * 100.0
    }

    fun addSavingsToGoal(amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val currentGoal = _state.value.activeGoal ?: withContext(Dispatchers.IO) {
                repository.getLatestGoal()
            }
            if (currentGoal == null) return@launch

            if (_state.value.activeGoal == null) {
                val monthsRemaining = calculateMonthsRemaining(currentGoal.deadlineTimestamp)
                _state.value = _state.value.copy(
                    activeGoal = currentGoal,
                    requiredMonthlySavings = calculateRequiredMonthlySavings(currentGoal, monthsRemaining),
                    progressPercent = calculateProgressPercent(currentGoal)
                )
            }

            val newSavings = currentGoal.currentSavings + amount
            withContext(Dispatchers.IO) {
                repository.updateGoalSavings(currentGoal.id, newSavings)
            }
            val updatedGoal = currentGoal.copy(currentSavings = newSavings)
            val monthsRemaining = calculateMonthsRemaining(updatedGoal.deadlineTimestamp)
            _state.value = _state.value.copy(
                activeGoal = updatedGoal,
                requiredMonthlySavings = calculateRequiredMonthlySavings(updatedGoal, monthsRemaining),
                progressPercent = calculateProgressPercent(updatedGoal)
            )
        }
    }

    fun setCategoryBudget(category: String, amount: Double, monthYear: String = currentMonthYear()) {
        viewModelScope.launch {
            val budgetCategory = BudgetCategory(
                id = budgetCategoryId(category, monthYear),
                categoryName = category,
                allocatedAmount = amount,
                monthYear = monthYear
            )
            withContext(Dispatchers.IO) {
                repository.upsertBudgetCategory(budgetCategory)
            }
            refresh(monthYear)
        }
    }

    private fun calculateMonthsRemaining(deadlineTimestamp: Long): Int {
        if (deadlineTimestamp <= 0L) return 0
        val zone = ZoneId.systemDefault()
        val today = YearMonth.from(Instant.now().atZone(zone))
        val deadline = YearMonth.from(Instant.ofEpochMilli(deadlineTimestamp).atZone(zone))
        val months = (deadline.year - today.year) * 12 + (deadline.monthValue - today.monthValue)
        return months.coerceAtLeast(0)
    }

    private fun currentMonthYear(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        return YearMonth.now().format(formatter)
    }

    private fun budgetCategoryId(category: String, monthYear: String): String {
        return "${monthYear}_${category.lowercase(Locale.US).replace(" ", "_")}"
    }

    private fun createDefaultGoal(): Goal {
        val now = Instant.now()
        val deadline = now.atZone(ZoneId.systemDefault()).plusMonths(12).toInstant().toEpochMilli()
        return Goal(
            id = "goal_macbook_m4",
            name = "MacBook Pro M4",
            targetAmount = 490_000.0,
            currentSavings = 0.0,
            deadlineTimestamp = deadline,
            createdAt = now.toEpochMilli()
        )
    }
}

data class BudgetUiState(
    val activeGoal: Goal? = null,
    val requiredMonthlySavings: Double = 0.0,
    val progressPercent: Double = 0.0,
    val categoryBudgets: List<BudgetCategorySummary> = emptyList()
)

