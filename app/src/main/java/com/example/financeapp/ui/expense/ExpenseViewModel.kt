package com.example.financeapp.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.repository.ExpenseRepository
import com.example.financeapp.util.AppEventBus
import com.example.financeapp.util.DataChangeEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val eventBus: AppEventBus,
) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseUiState())
    val state: StateFlow<ExpenseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            refreshExpenses()
        }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
            refreshExpenses()
            eventBus.send(DataChangeEvent.EXPENSE)
        }
    }

    fun setFilterCategory(category: String) {
        val current = _state.value
        _state.value = current.copy(
            selectedFilterCategory = category,
            filteredExpenses = filterByCategory(current.expenses, category)
        )
    }

    private suspend fun refreshExpenses() {
        val expenses = repository.getAllExpenses()
        val committedTotal = expenses
            .filter { it.spendingType.equals("COMMITTED", ignoreCase = true) }
            .sumOf { it.amountLkr }
        val discretionaryTotal = expenses
            .filter { it.spendingType.equals("DISCRETIONARY", ignoreCase = true) }
            .sumOf { it.amountLkr }
        val selectedCategory = _state.value.selectedFilterCategory
        _state.value = ExpenseUiState(
            expenses = expenses,
            filteredExpenses = filterByCategory(expenses, selectedCategory),
            committedTotal = committedTotal,
            discretionaryTotal = discretionaryTotal,
            selectedFilterCategory = selectedCategory
        )
    }

    private fun filterByCategory(expenses: List<Expense>, category: String): List<Expense> {
        return if (category.equals("All", ignoreCase = true)) {
            expenses
        } else {
            expenses.filter { it.category.equals(category, ignoreCase = true) }
        }
    }
}

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val committedTotal: Double = 0.0,
    val discretionaryTotal: Double = 0.0,
    val selectedFilterCategory: String = "All"
)
