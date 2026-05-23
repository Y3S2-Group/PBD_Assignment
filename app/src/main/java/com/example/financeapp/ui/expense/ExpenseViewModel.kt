package com.example.financeapp.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseUiState())
    val state: StateFlow<ExpenseUiState> = _state.asStateFlow()

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
            val expenses = repository.getAllExpenses()
            val committedTotal = expenses
                .filter { it.spendingType.equals("COMMITTED", ignoreCase = true) }
                .sumOf { it.amountLkr }
            val discretionaryTotal = expenses
                .filter { it.spendingType.equals("DISCRETIONARY", ignoreCase = true) }
                .sumOf { it.amountLkr }
            _state.value = ExpenseUiState(
                expenses = expenses,
                committedTotal = committedTotal,
                discretionaryTotal = discretionaryTotal
            )
        }
    }
}

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val committedTotal: Double = 0.0,
    val discretionaryTotal: Double = 0.0
)
