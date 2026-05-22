package presentation.expense

import data.model.Expense

sealed interface ExpenseUiState {
    object Loading : ExpenseUiState
    data class Success(val expenses: List<Expense>) : ExpenseUiState
    data class Error(val message: String) : ExpenseUiState
}
