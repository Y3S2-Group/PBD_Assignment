package com.example.financeapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.DashboardAnalytics
import com.example.financeapp.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val summary: DashboardAnalytics) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        _state.value = DashboardUiState.Loading
        viewModelScope.launch {
            val cached = repository.getCachedDashboardSummary()
            cached?.let {
                _state.value = DashboardUiState.Success(cached)
            }

            runCatching { repository.refreshDashboardSummary() }
                .onSuccess { summary ->
                    _state.value = DashboardUiState.Success(summary)
                }
                .onFailure {
                    if (cached == null) {
                        _state.value = DashboardUiState.Error(
                            message = "Dashboard analytics could not be loaded."
                        )
                    }
                }
        }
    }
}
