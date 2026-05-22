package presentation.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    init {
        loadExpenses()
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
    private val _addExpenseState = MutableStateFlow<AddExpenseState>(AddExpenseState.Idle)
    val addExpenseState: StateFlow<AddExpenseState> = _addExpenseState.asStateFlow()

    fun addExpense(amount: Double, description: String, category: data.model.ExpenseCategory) {
        viewModelScope.launch {
            _addExpenseState.value = AddExpenseState.Loading
            authRepository.currentUser?.let { user ->
                val expense = data.model.Expense(
                    amount = amount,
                    description = description,
                    userId = user.uid,
                    category = category
                )
                repository.addExpense(expense)
                    .onSuccess {
                        _addExpenseState.value = AddExpenseState.Success
                    }
                    .onFailure { error ->
                        _addExpenseState.value = AddExpenseState.Error(error.message ?: "Unknown error")
                    }
            }
        }
    }
}
