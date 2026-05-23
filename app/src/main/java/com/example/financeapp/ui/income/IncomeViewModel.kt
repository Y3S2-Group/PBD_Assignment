package com.example.financeapp.ui.income

import androidx.lifecycle.ViewModel
import com.example.financeapp.domain.model.Income
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface IncomeUiState {
    data object Loading : IncomeUiState
    data class Success(val entries: List<Income>) : IncomeUiState
}

class IncomeViewModel : ViewModel() {
    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state

    private val _totalLkr = MutableStateFlow(0.0)
    val totalLkr: StateFlow<Double> = _totalLkr

    fun loadIncomeHistory() {
        // TODO: Wire repository and update state/total.
    }
}

