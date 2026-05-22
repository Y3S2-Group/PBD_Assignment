package presentation.expense

sealed interface AddExpenseState {
    object Idle : AddExpenseState
    object Loading : AddExpenseState
    object Success : AddExpenseState
    data class Error(val message: String) : AddExpenseState
}
