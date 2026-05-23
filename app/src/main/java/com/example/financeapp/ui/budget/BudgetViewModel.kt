package com.example.financeapp.ui.budget

import androidx.lifecycle.ViewModel
import com.example.financeapp.domain.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(BudgetUiState())
    val state: StateFlow<BudgetUiState> = _state

    fun calculateRequiredMonthlySavings(goal: Goal, monthsRemaining: Int): Double {
        return 0.0
    }

    fun calculateProgressPercent(goal: Goal): Double {
        return 0.0
    }

    fun addFunds(goalId: String, amount: Double) {
        // TODO: Implement in phase 2.
    }
}

data class BudgetUiState(
    val goals: List<Goal> = emptyList()
)

