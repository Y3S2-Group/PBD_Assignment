package com.example.financeapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.Budget
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.repository.BudgetGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetGoalRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    fun calculateRequiredMonthlySavings(goal: Goal, monthsRemaining: Int): Double {
        if (monthsRemaining <= 0) return 0.0
        val remaining = (goal.targetAmount - goal.currentSavings).coerceAtLeast(0.0)
        return remaining / monthsRemaining
    }

    fun calculateProgressPercent(goal: Goal): Double {
        if (goal.targetAmount <= 0.0) return 0.0
        return (goal.currentSavings / goal.targetAmount) * 100.0
    }

    fun addFunds(goalId: String, amount: Double) {
        viewModelScope.launch {
            val goal = repository.getGoalById(goalId) ?: return@launch
            val newSavings = goal.currentSavings + amount
            repository.updateGoalSavings(goalId, newSavings)
            val updatedGoal = goal.copy(currentSavings = newSavings)
            _state.value = _state.value.copy(
                activeGoal = updatedGoal,
                progressPercent = calculateProgressPercent(updatedGoal),
                requiredMonthlySavings = _state.value.requiredMonthlySavings
            )
        }
    }

    fun setActiveGoal(goal: Goal, monthsRemaining: Int) {
        _state.value = _state.value.copy(
            activeGoal = goal,
            progressPercent = calculateProgressPercent(goal),
            requiredMonthlySavings = calculateRequiredMonthlySavings(goal, monthsRemaining)
        )
    }

    fun initializeGoal(goal: Goal, monthsRemaining: Int) {
        viewModelScope.launch {
            repository.insertGoal(goal)
            setActiveGoal(goal, monthsRemaining)
        }
    }

    fun updateBudgets(budgets: List<Budget>) {
        _state.value = _state.value.copy(budgets = budgets)
    }
}

data class BudgetUiState(
    val activeGoal: Goal? = null,
    val progressPercent: Double = 0.0,
    val requiredMonthlySavings: Double = 0.0,
    val budgets: List<Budget> = emptyList()
)

