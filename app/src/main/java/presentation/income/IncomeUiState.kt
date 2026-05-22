package presentation.income

import data.model.Income

sealed interface IncomeUiState {
    data object Loading : IncomeUiState
    data class Success(val incomes: List<Income>) : IncomeUiState
    data class Error(val message: String) : IncomeUiState
}
