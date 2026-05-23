package com.example.financeapp.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

sealed interface IncomeUiState {
    data object Loading : IncomeUiState
    data class Success(val entries: List<Income>) : IncomeUiState
}

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val repository: IncomeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    private val _totalLkr = MutableStateFlow(0.0)
    val totalLkr: StateFlow<Double> = _totalLkr.asStateFlow()

    fun loadIncomeHistory() {
        _state.value = IncomeUiState.Loading
        viewModelScope.launch {
            val entries = repository.getAllIncomes()
            _state.value = IncomeUiState.Success(entries)
            _totalLkr.value = entries.sumOf { it.amountLKR }
        }
    }

    fun addIncome(
        amount: Double,
        currency: String,
        sourceType: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val amountLkr = amount * exchangeRateFor(currency)
            val income = Income(
                id = "inc_${UUID.randomUUID()}",
                amount = amount,
                currency = currency,
                amountLKR = amountLkr,
                sourceType = sourceType,
                date = System.currentTimeMillis()
            )
            repository.insertIncome(income)
            val updated = repository.getAllIncomes()
            _state.value = IncomeUiState.Success(updated)
            _totalLkr.value = updated.sumOf { it.amountLKR }
        }
    }

    private fun exchangeRateFor(currency: String): Double {
        return when (currency.uppercase()) {
            "USD" -> 300.0
            "USDT" -> 300.0
            "ETH" -> 900_000.0
            "LKR" -> 1.0
            else -> 1.0
        }
    }
}
