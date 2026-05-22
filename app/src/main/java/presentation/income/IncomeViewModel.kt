package presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import data.model.Currency
import data.model.Income
import data.model.IncomeSource
import domain.repository.AuthRepository
import domain.repository.IncomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import utils.getCurrentMonthRange
import javax.inject.Inject

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val repository: IncomeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val uiState: StateFlow<IncomeUiState> = _uiState.asStateFlow()

    private val _summary = MutableStateFlow(IncomeSummary())
    val summary: StateFlow<IncomeSummary> = _summary.asStateFlow()

    private val _addIncomeState = MutableStateFlow<AddIncomeState>(AddIncomeState.Idle)
    val addIncomeState: StateFlow<AddIncomeState> = _addIncomeState.asStateFlow()

    init {
        loadIncomes()
        loadMonthlySummary()
    }

    private fun loadIncomes() {
        viewModelScope.launch {
            authRepository.currentUser?.let { user ->
                repository.getIncomes(user.uid).collect { incomes ->
                    _uiState.value = IncomeUiState.Success(incomes)
                }
            }
        }
    }

    fun loadMonthlySummary() {
        viewModelScope.launch {
            authRepository.currentUser?.let { user ->
                val (start, end) = getCurrentMonthRange()
                val totalByCurrency = repository.getTotalIncomeByCurrency(user.uid, start, end)
                val bySource = repository.getIncomeBySource(user.uid, start, end)

                _summary.value = IncomeSummary(
                    totalByCurrency = totalByCurrency,
                    bySource = bySource
                )
            }
        }
    }

    fun addIncome(
        amount: Double,
        description: String,
        source: IncomeSource,
        currency: Currency
    ) {
        viewModelScope.launch {
            _addIncomeState.value = AddIncomeState.Loading
            authRepository.currentUser?.let { user ->
                val income = Income(
                    amount = amount,
                    description = description,
                    source = source,
                    currency = currency,
                    userId = user.uid
                )
                repository.addIncome(income)
                    .onSuccess {
                        _addIncomeState.value = AddIncomeState.Success
                        loadMonthlySummary()
                    }
                    .onFailure { error ->
                        _addIncomeState.value = AddIncomeState.Error(
                            error.message ?: "Unknown error"
                        )
                    }
            }
        }
    }
}
