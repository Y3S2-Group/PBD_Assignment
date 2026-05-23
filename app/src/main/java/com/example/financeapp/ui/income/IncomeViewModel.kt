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
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

sealed interface IncomeUiState {
    data object Loading : IncomeUiState
    data class Success(val entries: List<Income>) : IncomeUiState
}

enum class IncomePeriod {
    WEEK,
    MONTH,
    YEAR
}

data class SourceBreakdown(
    val sourceType: String,
    val label: String? = null,
    val amountLkr: Double
)

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val repository: IncomeRepository
) : ViewModel() {
    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    private val _totalLkr = MutableStateFlow(0.0)
    val totalLkr: StateFlow<Double> = _totalLkr.asStateFlow()

    private val _sourceBreakdown = MutableStateFlow<List<SourceBreakdown>>(emptyList())
    val sourceBreakdown: StateFlow<List<SourceBreakdown>> = _sourceBreakdown.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(IncomePeriod.MONTH)
    val selectedPeriod: StateFlow<IncomePeriod> = _selectedPeriod.asStateFlow()

    private var cachedEntries: List<Income> = emptyList()

    fun loadIncomeHistory() {
        _state.value = IncomeUiState.Loading
        viewModelScope.launch {
            val entries = repository.getAllIncomes()
            cachedEntries = entries
            updateForPeriod(entries)
        }
    }

    fun addIncome(
        amount: Double,
        currency: String,
        sourceType: String,
        sourceLabel: String?,
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
                sourceLabel = sourceLabel,
                date = System.currentTimeMillis()
            )
            repository.insertIncome(income)
            val updated = repository.getAllIncomes()
            cachedEntries = updated
            updateForPeriod(updated)
        }
    }

    fun updateIncome(
        id: String,
        amount: Double,
        currency: String,
        sourceType: String,
        sourceLabel: String?,
        date: Long
    ) {
        viewModelScope.launch {
            val amountLkr = amount * exchangeRateFor(currency)
            val income = Income(
                id = id,
                amount = amount,
                currency = currency,
                amountLKR = amountLkr,
                sourceType = sourceType,
                sourceLabel = sourceLabel,
                date = date
            )
            repository.updateIncome(income)
            val updated = repository.getAllIncomes()
            cachedEntries = updated
            updateForPeriod(updated)
        }
    }

    fun deleteIncome(id: String) {
        viewModelScope.launch {
            repository.deleteIncome(id)
            val updated = repository.getAllIncomes()
            cachedEntries = updated
            updateForPeriod(updated)
        }
    }

    fun setPeriod(period: IncomePeriod) {
        if (period == _selectedPeriod.value) return
        _selectedPeriod.value = period
        updateForPeriod(cachedEntries)
    }

    private fun updateForPeriod(entries: List<Income>) {
        val (startInclusive, endInclusive) = periodRangeMillis(_selectedPeriod.value)
        val filtered = entries.filter { it.date in startInclusive..endInclusive }
        _state.value = IncomeUiState.Success(filtered)
        _totalLkr.value = filtered.sumOf { it.amountLKR }
        _sourceBreakdown.value = computeBreakdown(filtered)
    }

    private fun periodRangeMillis(period: IncomePeriod): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startDate = when (period) {
            IncomePeriod.WEEK -> {
                val weekFields = WeekFields.of(Locale.getDefault())
                today.with(weekFields.dayOfWeek(), 1)
            }
            IncomePeriod.MONTH -> today.withDayOfMonth(1)
            IncomePeriod.YEAR -> today.withDayOfYear(1)
        }
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = System.currentTimeMillis()
        return startMillis to endMillis
    }

    private fun computeBreakdown(entries: List<Income>): List<SourceBreakdown> {
        if (entries.isEmpty()) return emptyList()
        val orderedSources = listOf("SALARY", "FREELANCE", "ADSENSE", "CRYPTO")
        val totalsBySource = entries.groupBy { it.sourceType.uppercase() }
            .mapValues { (_, incomes) -> incomes.sumOf { it.amountLKR } }
        val breakdown = orderedSources.mapNotNull { source ->
            val amount = totalsBySource[source] ?: 0.0
            if (amount > 0.0) SourceBreakdown(sourceType = source, amountLkr = amount) else null
        }.toMutableList()

        val otherGroups = entries.filter { it.sourceType.uppercase() !in orderedSources }
            .groupBy { income ->
                val type = income.sourceType.uppercase()
                if (type == "CUSTOM") {
                    income.sourceLabel?.trim().takeIf { !it.isNullOrBlank() } ?: "Custom"
                } else {
                    type
                }
            }
            .toSortedMap()

        otherGroups.forEach { (label, incomes) ->
            val type = incomes.first().sourceType.uppercase()
            val amount = incomes.sumOf { it.amountLKR }
            if (amount > 0.0) {
                val resolvedLabel = if (type == "CUSTOM") label else label.lowercase().replaceFirstChar { it.uppercase() }
                breakdown.add(SourceBreakdown(sourceType = type, label = resolvedLabel, amountLkr = amount))
            }
        }

        return breakdown
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
