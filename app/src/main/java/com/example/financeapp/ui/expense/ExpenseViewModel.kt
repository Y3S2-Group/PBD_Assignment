package com.example.financeapp.ui.expense

import androidx.lifecycle.ViewModel
import com.example.financeapp.domain.model.Expense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(ExpenseUiState())
    val state: StateFlow<ExpenseUiState> = _state

    fun addExpense(expense: Expense) {
        // TODO: Implement in TDD phase 2.
    }
}

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val committedTotal: Double = 0.0,
    val discretionaryTotal: Double = 0.0
)

