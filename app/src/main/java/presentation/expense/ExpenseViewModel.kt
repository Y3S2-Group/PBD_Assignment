package presentation.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Expense
import data.model.ExpenseCategory
import data.model.ExpenseType
import domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import utils.getCurrentMonthRange
import javax.inject.Inject

// Placeholder for AuthRepository and ExpenseUiState
import presentation.expense.ExpenseUiState
import domain.repository.AuthRepository

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Loading)
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private val _summary = MutableStateFlow(ExpenseSummary())
    val summary: StateFlow<ExpenseSummary> = _summary.asStateFlow()

    init {
        loadExpenses()
        loadMonthlySummary()
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            authRepository.currentUser?.let { user ->
                repository.getExpenses(user.uid).collect { expenses ->
                    _uiState.value = ExpenseUiState.Success(expenses)
                }
            }
        }
    }

    fun loadMonthlySummary() {
        viewModelScope.launch {
            authRepository.currentUser?.let { user ->
                val (start, end) = getCurrentMonthRange()
                val total = repository.getTotalExpenses(user.uid, start, end)
                val byCategory = repository.getExpensesByCategory(user.uid, start, end)
                val allExpenses = repository.getExpenses(user.uid, start, end)
                val discretionary = allExpenses
                    .filter { it.type == ExpenseType.DISCRETIONARY }
                    .sumOf { it.amount }
                val committed = allExpenses
                    .filter { it.type == ExpenseType.COMMITTED }
                    .sumOf { it.amount }

                _summary.value = ExpenseSummary(
                    total = total,
                    byCategory = byCategory,
                    discretionaryTotal = discretionary,
                    committedTotal = committed
                )
            }
        }
    }

    private val _addExpenseState = MutableStateFlow<AddExpenseState>(AddExpenseState.Idle)
    val addExpenseState: StateFlow<AddExpenseState> = _addExpenseState.asStateFlow()

    fun addExpense(amount: Double, description: String, category: ExpenseCategory) {
        viewModelScope.launch {
            _addExpenseState.value = AddExpenseState.Loading
            authRepository.currentUser?.let { user ->
                val expense = Expense(
                    amount = amount,
                    description = description,
                    category = category,
                    userId = user.uid
                )
                repository.addExpense(expense)
                    .onSuccess {
                        _addExpenseState.value = AddExpenseState.Success
                        loadMonthlySummary()
                    }
                    .onFailure { error ->
                        _addExpenseState.value = AddExpenseState.Error(error.message ?: "Unknown error")
                    }
            }
        }
    }
}
