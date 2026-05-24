package com.example.financeapp.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.repository.ExpenseRepository
import com.example.financeapp.util.AppEventBus
import com.example.financeapp.util.DataChangeEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
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

    fun saveExpense(
        amountLkr: Double,
        category: String,
        spendingType: String,
        paymentMethod: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val currentEdit = _state.value.expenseToEdit
        val expense = if (currentEdit != null) {
            currentEdit.copy(
                amountLkr = amountLkr,
                category = category,
                spendingType = spendingType,
                paymentMethod = paymentMethod,
                timestamp = timestamp
            )
        } else {
            Expense(
                id = "exp_${UUID.randomUUID()}",
                amountLkr = amountLkr,
                category = category,
                spendingType = spendingType,
                paymentMethod = paymentMethod,
                timestamp = timestamp
            )
        }

        viewModelScope.launch {
            if (currentEdit != null) {
                repository.updateExpense(expense)
            } else {
                repository.insertExpense(expense)
            }
            _state.value = _state.value.copy(expenseToEdit = null)
            refreshExpenses()
            eventBus.send(DataChangeEvent.EXPENSE)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            refreshExpenses()
            eventBus.send(DataChangeEvent.EXPENSE)
        }
    }

    fun setExpenseToEdit(expense: Expense?) {
        _state.value = _state.value.copy(expenseToEdit = expense)
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
    val selectedFilterCategory: String = "All",
    val expenseToEdit: Expense? = null
)
