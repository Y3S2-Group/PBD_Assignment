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
}
