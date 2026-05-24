package com.example.financeapp.ui.income

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.model.RecurringIncome
import com.example.financeapp.domain.repository.IncomeRepository
import com.example.financeapp.util.AppEventBus
import com.example.financeapp.util.DataChangeEvent
import com.example.financeapp.util.RecurringIncomeReminderService
import com.example.financeapp.util.RecurringIncomeScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI state types ────────────────────────────────────────────────────────────

sealed interface IncomeUiState {
    data object Loading : IncomeUiState
    data class Success(val entries: List<Income>) : IncomeUiState
}

enum class IncomePeriod { WEEK, MONTH, YEAR }

data class SourceBreakdown(
    val sourceType: String,
    val label: String? = null,
    val amountLkr: Double,
)

/** Payload for adding or updating an income entry. */
data class AddIncomeRequest(
    val amount: Double,
    val currency: String,
    val sourceType: String,
    val sourceLabel: String? = null,
    val notes: String? = null,
    val projectRef: String? = null,
    /** If null, the hardcoded default rate is used. */
    val customExchangeRate: Double? = null,
    val isRecurring: Boolean = false,
    val invoicePaid: Boolean = true,
    val date: Long = System.currentTimeMillis(),
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val repository: IncomeRepository,
    @ApplicationContext private val appContext: Context,
    private val eventBus: AppEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    private val _totalLkr = MutableStateFlow(0.0)
    val totalLkr: StateFlow<Double> = _totalLkr.asStateFlow()

    private val _sourceBreakdown = MutableStateFlow<List<SourceBreakdown>>(emptyList())
    val sourceBreakdown: StateFlow<List<SourceBreakdown>> = _sourceBreakdown.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(IncomePeriod.MONTH)
    val selectedPeriod: StateFlow<IncomePeriod> = _selectedPeriod.asStateFlow()

    /** Currently active source-type filters. Empty set = show all. */
    private val _selectedSourceFilter = MutableStateFlow<Set<String>>(emptySet())
    val selectedSourceFilter: StateFlow<Set<String>> = _selectedSourceFilter.asStateFlow()

    /** Active recurring income templates. */
    private val _recurringIncomes = MutableStateFlow<List<RecurringIncome>>(emptyList())
    val recurringIncomes: StateFlow<List<RecurringIncome>> = _recurringIncomes.asStateFlow()

    /**
     * Percentage change of this period's LKR total vs the previous same-length period.
     * Null when the previous period had zero income (N/A).
     */
    private val _periodChangePercent = MutableStateFlow<Double?>(null)
    val periodChangePercent: StateFlow<Double?> = _periodChangePercent.asStateFlow()

    /** Full sorted cache — re-filtered in [updateForPeriod]. */
    private var cachedEntries: List<Income> = emptyList()

    init {
        loadIncomeHistory()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun loadIncomeHistory() {
        _state.value = IncomeUiState.Loading
        viewModelScope.launch {
            cachedEntries = repository.getAllIncomes()
            _recurringIncomes.value = repository.getActiveRecurringIncomes()
            updateForPeriod(cachedEntries)
        }
    }

    fun addIncome(req: AddIncomeRequest) {
        viewModelScope.launch {
            val rate = req.customExchangeRate?.takeIf { it > 0 }
                ?: defaultExchangeRateFor(req.currency)
            val amountLkr = req.amount * rate
            val income = Income(
                id = "inc_${UUID.randomUUID()}",
                amount = req.amount,
                currency = req.currency,
                amountLKR = amountLkr,
                sourceType = req.sourceType,
                sourceLabel = req.sourceLabel,
                date = req.date,
                notes = req.notes,
                projectRef = req.projectRef,
                exchangeRate = rate,
                isRecurring = req.isRecurring,
                invoicePaid = req.invoicePaid,
            )
            repository.insertIncome(income)
            cachedEntries = repository.getAllIncomes()
            updateForPeriod(cachedEntries)
            eventBus.send(DataChangeEvent.INCOME)
        }
    }

    fun updateIncome(id: String, req: AddIncomeRequest) {
        viewModelScope.launch {
            val rate = req.customExchangeRate?.takeIf { it > 0 }
                ?: defaultExchangeRateFor(req.currency)
            val income = Income(
                id = id,
                amount = req.amount,
                currency = req.currency,
                amountLKR = req.amount * rate,
                sourceType = req.sourceType,
                sourceLabel = req.sourceLabel,
                date = req.date,
                notes = req.notes,
                projectRef = req.projectRef,
                exchangeRate = rate,
                isRecurring = req.isRecurring,
                invoicePaid = req.invoicePaid,
            )
            repository.updateIncome(income)
            cachedEntries = repository.getAllIncomes()
            updateForPeriod(cachedEntries)
            eventBus.send(DataChangeEvent.INCOME)
        }
    }

    fun deleteIncome(id: String) {
        viewModelScope.launch {
            repository.deleteIncome(id)
            cachedEntries = repository.getAllIncomes()
            updateForPeriod(cachedEntries)
            eventBus.send(DataChangeEvent.INCOME)
        }
    }

    fun setPeriod(period: IncomePeriod) {
        if (period == _selectedPeriod.value) return
        _selectedPeriod.value = period
        updateForPeriod(cachedEntries)
    }

    fun toggleSourceFilter(sourceType: String) {
        val current = _selectedSourceFilter.value.toMutableSet()
        if (!current.add(sourceType)) current.remove(sourceType)
        _selectedSourceFilter.value = current
        updateForPeriod(cachedEntries)
    }

    fun clearSourceFilter() {
        _selectedSourceFilter.value = emptySet()
        updateForPeriod(cachedEntries)
    }

    // ── Recurring income ──────────────────────────────────────────────────────

    fun addRecurringIncome(
        sourceType: String,
        sourceLabel: String?,
        currency: String,
        defaultAmount: Double,
        dayOfMonth: Int,
    ) {
        viewModelScope.launch {
            val recurring = RecurringIncome(
                id = "rec_${UUID.randomUUID()}",
                sourceType = sourceType,
                sourceLabel = sourceLabel,
                currency = currency,
                defaultAmount = defaultAmount,
                dayOfMonth = dayOfMonth.coerceIn(1, 28),
                isActive = true,
                createdAt = System.currentTimeMillis(),
            )
            repository.insertRecurringIncome(recurring)
            RecurringIncomeScheduler.schedule(appContext, recurring)
            _recurringIncomes.value = repository.getActiveRecurringIncomes()
        }
    }

    fun deactivateRecurringIncome(id: String) {
        viewModelScope.launch {
            repository.deactivateRecurringIncome(id)
            RecurringIncomeScheduler.cancel(appContext, id)
            _recurringIncomes.value = repository.getActiveRecurringIncomes()
        }
    }

    /** Re-schedules all active recurring alarms (e.g. after the app updates). */
    fun rescheduleAllRecurringAlarms() {
        val intent = Intent(appContext, RecurringIncomeReminderService::class.java).apply {
            action = RecurringIncomeReminderService.ACTION_RESCHEDULE
        }
        appContext.startService(intent)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun updateForPeriod(entries: List<Income>) {
        val zone = ZoneId.systemDefault()
        val (startInclusive, endInclusive) = periodRangeMillis(_selectedPeriod.value)
        val (prevStart, prevEnd) = previousPeriodRangeMillis(_selectedPeriod.value, zone)

        val periodEntries = entries.filter { it.date in startInclusive..endInclusive }
        val activeFilter = _selectedSourceFilter.value

        val displayed = if (activeFilter.isEmpty()) periodEntries
        else periodEntries.filter { it.sourceType.uppercase() in activeFilter }

        _state.value = IncomeUiState.Success(displayed)
        _totalLkr.value = periodEntries.sumOf { it.amountLKR }
        _sourceBreakdown.value = computeBreakdown(periodEntries)

        // Month-over-month comparison (always vs previous calendar period)
        val currentTotal = periodEntries.sumOf { it.amountLKR }
        val prevEntries = entries.filter { it.date in prevStart..prevEnd }
        val prevTotal = prevEntries.sumOf { it.amountLKR }
        _periodChangePercent.value = if (prevTotal > 0)
            ((currentTotal - prevTotal) / prevTotal) * 100.0
        else null
    }

    private fun periodRangeMillis(period: IncomePeriod): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startDate = when (period) {
            IncomePeriod.WEEK -> today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
            IncomePeriod.MONTH -> today.withDayOfMonth(1)
            IncomePeriod.YEAR -> today.withDayOfYear(1)
        }
        return startDate.atStartOfDay(zone).toInstant().toEpochMilli() to System.currentTimeMillis()
    }

    private fun previousPeriodRangeMillis(
        period: IncomePeriod,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        val (startDate, endDate) = when (period) {
            IncomePeriod.WEEK -> {
                val weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                weekStart.minusWeeks(1) to weekStart.minusDays(1)
            }

            IncomePeriod.MONTH -> {
                val thisMonthStart = today.withDayOfMonth(1)
                thisMonthStart.minusMonths(1) to thisMonthStart.minusDays(1)
            }

            IncomePeriod.YEAR -> {
                val thisYearStart = today.withDayOfYear(1)
                thisYearStart.minusYears(1) to thisYearStart.minusDays(1)
            }
        }
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = endDate.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun computeBreakdown(entries: List<Income>): List<SourceBreakdown> {
        if (entries.isEmpty()) return emptyList()
        val orderedSources = listOf("SALARY", "FREELANCE", "ADSENSE", "CRYPTO")
        val totalsBySource = entries.groupBy { it.sourceType.uppercase() }
            .mapValues { (_, list) -> list.sumOf { it.amountLKR } }

        val breakdown = orderedSources.mapNotNull { source ->
            val amount = totalsBySource[source] ?: 0.0
            if (amount > 0.0) SourceBreakdown(sourceType = source, amountLkr = amount) else null
        }.toMutableList()

        val otherGroups = entries
            .filter { it.sourceType.uppercase() !in orderedSources }
            .groupBy { income ->
                val type = income.sourceType.uppercase()
                if (type == "CUSTOM") income.sourceLabel?.trim().takeIf { !it.isNullOrBlank() } ?: "Custom"
                else type
            }
            .toSortedMap()

        otherGroups.forEach { (label, incomes) ->
            val type = incomes.first().sourceType.uppercase()
            val amount = incomes.sumOf { it.amountLKR }
            if (amount > 0.0) {
                val resolved =
                    if (type == "CUSTOM") label
                    else label.lowercase().replaceFirstChar { it.uppercase() }
                breakdown.add(SourceBreakdown(sourceType = type, label = resolved, amountLkr = amount))
            }
        }
        return breakdown
    }

    internal fun defaultExchangeRateFor(currency: String): Double = when (currency.uppercase()) {
        "USD" -> 300.0
        "USDT" -> 300.0
        "ETH" -> 900_000.0
        "LKR" -> 1.0
        else -> 1.0
    }
}
