package presentation.income

sealed interface AddIncomeState {
    data object Idle : AddIncomeState
    data object Loading : AddIncomeState
    data object Success : AddIncomeState
    data class Error(val message: String) : AddIncomeState
}
