package com.example.financeapp.ui.income

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.model.RecurringIncome
import com.example.financeapp.ui.components.GlobalTopAppBar
import com.example.financeapp.ui.dashboard.DashboardViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun IncomeScreen(
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: IncomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dashVm: DashboardViewModel = hiltViewModel()
    val dashState by dashVm.state.collectAsState()
    val totalLkr by viewModel.totalLkr.collectAsState()
    val sourceBreakdown by viewModel.sourceBreakdown.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedSourceFilter by viewModel.selectedSourceFilter.collectAsState()
    val recurringIncomes by viewModel.recurringIncomes.collectAsState()
    val periodChangePercent by viewModel.periodChangePercent.collectAsState()
    val errorMessage = (state as? IncomeUiState.Success)?.errorMessage

    var showBottomSheet by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }
    var deleteTarget by remember { mutableStateOf<Income?>(null) }

    val incomes = (state as? IncomeUiState.Success)?.entries.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        GlobalTopAppBar(
            title = "Income",
            subtitle = null,
            healthScore = dashState.healthScore,
            localProfilePhotoPath = null,
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                IncomeHeader(
                    totalLkr = totalLkr,
                    selectedPeriod = selectedPeriod,
                    periodChangePercent = periodChangePercent,
                    onPeriodChange = viewModel::setPeriod,
                )
            }
            item { SourceBreakdownChart(breakdown = sourceBreakdown, totalLkr = totalLkr) }

            // Recurring income section
            if (recurringIncomes.isNotEmpty()) {
                item {
                    RecurringIncomeSection(
                        recurringIncomes = recurringIncomes,
                        onDeactivate = viewModel::deactivateRecurringIncome,
                    )
                }
            }

            item {
                IncomeHistoryList(
                    incomes = incomes,
                    selectedSourceFilter = selectedSourceFilter,
                    onToggleFilter = viewModel::toggleSourceFilter,
                    onClearFilter = viewModel::clearSourceFilter,
                    onEdit = { income ->
                        editingIncome = income
                        showBottomSheet = true
                    },
                    onDelete = { income -> deleteTarget = income },
                )
            }
        }
        }

        // FAB — add income
        FloatingActionButton(
            onClick = {
                editingIncome = null
                showBottomSheet = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 88.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add income")
        }

        if (showBottomSheet) {
            AddIncomeBottomSheet(
                onDismiss = {
                    showBottomSheet = false
                    editingIncome = null
                    viewModel.clearError()
                },
                initialIncome = editingIncome,
                errorMessage = errorMessage,
                onSave = { req ->
                    viewModel.addIncome(req)
                },
                onUpdate = { id, req ->
                    viewModel.updateIncome(id, req)
                },
            )
        }

        // Close bottom sheet only if success and no error
        LaunchedEffect(state) {
            if (state is IncomeUiState.Success && (state as IncomeUiState.Success).errorMessage == null) {
                if (showBottomSheet) {
                    showBottomSheet = false
                    editingIncome = null
                }
            }
        }

        if (deleteTarget != null) {
            DeleteIncomeDialog(
                income = deleteTarget!!,
                onConfirm = {
                    viewModel.deleteIncome(deleteTarget!!.id)
                    deleteTarget = null
                },
                onDismiss = { deleteTarget = null },
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun IncomeHeader(
    totalLkr: Double,
    selectedPeriod: IncomePeriod,
    periodChangePercent: Double?,
    onPeriodChange: (IncomePeriod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "Revenue Streams",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = periodTitle(selectedPeriod),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "LKR ${formatAmount(totalLkr)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                if (periodChangePercent != null) {
                    val isUp = periodChangePercent >= 0
                    val color = if (isUp) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = if (isUp) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${if (isUp) "+" else ""}${"%.1f".format(periodChangePercent)}% vs prev",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                        )
                    }
                } else {
                    Text(
                        text = "Updated just now",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(IncomePeriod.WEEK, IncomePeriod.MONTH, IncomePeriod.YEAR).forEach { period ->
                PeriodChip(
                    label = period.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                )
            }
        }
    }
}

// ── Source breakdown chart ────────────────────────────────────────────────────

@Composable
private fun SourceBreakdownChart(breakdown: List<SourceBreakdown>, totalLkr: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            if (breakdown.isEmpty() || totalLkr <= 0.0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No income data yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    breakdown.forEachIndexed { index, item ->
                        val fraction = (item.amountLkr / totalLkr).toFloat().coerceIn(0f, 1f)
                        val shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            breakdown.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                        BreakdownSegment(
                            fraction = fraction,
                            color = sourceTintFor(item.sourceType),
                            label = "${(fraction * 100).roundToInt()}%",
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = shape,
                        )
                    }
                }
            }
        }
        if (breakdown.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { item ->
                            LegendItem(
                                label = sourceLabelFor(item.sourceType, item.label),
                                color = sourceTintFor(item.sourceType),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Recurring income section ──────────────────────────────────────────────────

@Composable
private fun RecurringIncomeSection(
    recurringIncomes: List<RecurringIncome>,
    onDeactivate: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Recurring Income",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        recurringIncomes.forEach { recurring ->
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = sourceLabelFor(recurring.sourceType, recurring.sourceLabel),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Day ${recurring.dayOfMonth} each month · ${recurring.currency} ${formatAmount(recurring.defaultAmount)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Deactivate",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDeactivate(recurring.id) },
                    )
                }
            }
        }
    }
}

// ── Income history list ───────────────────────────────────────────────────────

@Composable
private fun IncomeHistoryList(
    incomes: List<Income>,
    selectedSourceFilter: Set<String>,
    onToggleFilter: (String) -> Unit,
    onClearFilter: () -> Unit,
    onEdit: (Income) -> Unit,
    onDelete: (Income) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Income History",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (selectedSourceFilter.isNotEmpty()) {
                TextButton(onClick = onClearFilter) {
                    Text(
                        text = "Clear filter",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Source filter chips
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("SALARY", "FREELANCE", "ADSENSE", "CRYPTO", "CUSTOM").forEach { source ->
                val selected = source in selectedSourceFilter
                FilterChip(
                    selected = selected,
                    onClick = { onToggleFilter(source) },
                    label = {
                        Text(
                            text = source.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        if (incomes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (selectedSourceFilter.isNotEmpty()) "No entries match the selected filter"
                    else "No income entries yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                incomes.forEach { income ->
                    IncomeListItem(
                        income = income,
                        onEdit = { onEdit(income) },
                        onDelete = { onDelete(income) },
                    )
                }
            }
        }
    }
}

// ── Income list item ──────────────────────────────────────────────────────────

@Composable
private fun IncomeListItem(income: Income, onEdit: () -> Unit, onDelete: () -> Unit) {
    val entry = income.toIncomeEntry()
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(entry.tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, entry.tint.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = entry.icon, contentDescription = entry.title, tint = entry.tint)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            entry.badges.forEach { badge ->
                                Spacer(Modifier.width(6.dp))
                                StatusBadge(badge)
                            }
                        }
                        Text(
                            text = entry.secondaryAmount,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.primaryAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = entry.dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).clickable { onEdit() },
                        )
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp).clickable { onDelete() },
                        )
                    }
                }
            }

            // Extra detail rows
            val details = buildList {
                if (!income.projectRef.isNullOrBlank()) add("📁 ${income.projectRef}")
                if (!income.notes.isNullOrBlank()) add("📝 ${income.notes}")
            }
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                details.forEach { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ── Add / Edit bottom sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeBottomSheet(
    onDismiss: () -> Unit,
    initialIncome: Income?,
    errorMessage: String?,
    onSave: (AddIncomeRequest) -> Unit,
    onUpdate: (String, AddIncomeRequest) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Form state ────────────────────────────────────────────────────────────
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCurrency by rememberSaveable { mutableStateOf("LKR") }
    var selectedSource by rememberSaveable { mutableStateOf("SALARY") }
    var customSourceLabel by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var projectRef by rememberSaveable { mutableStateOf("") }
    var exchangeRateText by rememberSaveable { mutableStateOf("") }
    var isRecurring by rememberSaveable { mutableStateOf(false) }
    var recurringDay by rememberSaveable { mutableStateOf(25f) }  // slider value
    var invoicePaid by rememberSaveable { mutableStateOf(true) }
    var selectedDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Pre-fill for edit mode
    LaunchedEffect(initialIncome?.id) {
        if (initialIncome != null) {
            amountText = formatAmountInput(initialIncome.amount)
            selectedCurrency = initialIncome.currency
            selectedSource = initialIncome.sourceType.uppercase()
            customSourceLabel = initialIncome.sourceLabel.orEmpty()
            notes = initialIncome.notes.orEmpty()
            projectRef = initialIncome.projectRef.orEmpty()
            exchangeRateText = if (initialIncome.exchangeRate != 1.0) formatAmountInput(initialIncome.exchangeRate) else ""
            isRecurring = initialIncome.isRecurring
            invoicePaid = initialIncome.invoicePaid
            selectedDateMillis = initialIncome.date
        } else {
            amountText = ""; selectedCurrency = "LKR"; selectedSource = "SALARY"
            customSourceLabel = ""; notes = ""; projectRef = ""; exchangeRateText = ""
            isRecurring = false; invoicePaid = true
            selectedDateMillis = System.currentTimeMillis()
        }
    }

    val amountValue = amountText.toDoubleOrNull()

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis =
                        datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (initialIncome == null) "Add Income" else "Edit Income",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(4.dp)
                        .clickable { onDismiss() },
                )
            }

            // Amount display + keypad
            IncomeAmountDisplay(currency = selectedCurrency, value = amountText)

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }

            IncomeKeypad(value = amountText, onValueChange = { amountText = it })

            // Currency
            LabelledSection("Currency") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("LKR", "USD", "USDT", "ETH").forEach { currency ->
                        FilterChip(
                            selected = selectedCurrency == currency,
                            onClick = { selectedCurrency = currency },
                            label = { Text(currency) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }

            // Exchange rate (removed)

            // Source
            LabelledSection("Source") {
                val sources = listOf(
                    SourceOption("SALARY", "Salary", Icons.Outlined.Work),
                    SourceOption("FREELANCE", "Freelance", Icons.Outlined.BusinessCenter),
                    SourceOption("ADSENSE", "AdSense", Icons.Outlined.Campaign),
                    SourceOption("CRYPTO", "Crypto", Icons.Outlined.CurrencyBitcoin),
                    SourceOption("CUSTOM", "Custom", Icons.Outlined.MoreHoriz),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = selectedSource == option.key,
                                    onClick = { selectedSource = option.key },
                                    leadingIcon = {
                                        Icon(option.icon, contentDescription = option.label)
                                    },
                                    label = { Text(option.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // Custom source label
            AnimatedVisibility(visible = selectedSource == "CUSTOM") {
                TextField(
                    value = customSourceLabel,
                    onValueChange = { customSourceLabel = it },
                    placeholder = { Text("Custom source name") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Freelance: project ref + invoice paid
            AnimatedVisibility(visible = selectedSource == "FREELANCE") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = projectRef,
                        onValueChange = { projectRef = it },
                        placeholder = { Text("Project / Client name (optional)") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Invoice Paid", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (invoicePaid) "Payment received" else "Invoice outstanding",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (invoicePaid) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error,
                            )
                        }
                        Switch(
                            checked = invoicePaid,
                            onCheckedChange = { invoicePaid = it },
                        )
                    }
                }
            }

            // Notes
            TextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Add a note (optional)") },
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Date picker row
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                cornerRadius = 12.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = formatDateLong(selectedDateMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Recurring toggle + day picker
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Recurring Income", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Repeats monthly on a fixed day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
                AnimatedVisibility(visible = isRecurring) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Reminder on day ${recurringDay.roundToInt()} of each month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = recurringDay,
                            onValueChange = { recurringDay = it },
                            valueRange = 1f..28f,
                            steps = 26,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Save / Update button
            val hasCustomLabel = selectedSource != "CUSTOM" || customSourceLabel.isNotBlank()
            val canSubmit = amountValue != null && amountValue > 0 && hasCustomLabel
            Button(
                onClick = {
                    if (!canSubmit) return@Button
                    val label = if (selectedSource == "CUSTOM") customSourceLabel.trim() else null
                    val req = AddIncomeRequest(
                        amount = amountValue!!,
                        currency = selectedCurrency,
                        sourceType = selectedSource,
                        sourceLabel = label,
                        notes = notes.ifBlank { null },
                        projectRef = if (selectedSource == "FREELANCE") projectRef.ifBlank { null } else null,
                        customExchangeRate = exchangeRateText.toDoubleOrNull(),
                        isRecurring = isRecurring,
                        invoicePaid = invoicePaid,
                        date = selectedDateMillis,
                    )
                    if (initialIncome == null) onSave(req)
                    else onUpdate(initialIncome.id, req)
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    text = if (initialIncome == null) "Save Income" else "Update Income",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

// ── Delete dialog ─────────────────────────────────────────────────────────────

@Composable
private fun DeleteIncomeDialog(income: Income, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete income?") },
        text = {
            Text("This will permanently remove the ${sourceLabelFor(income.sourceType, income.sourceLabel)} entry of LKR ${formatAmount(income.amountLKR)}.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun LabelledSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun StatusBadge(badge: IncomeBadge) {
    Box(
        modifier = Modifier
            .background(badge.color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .border(1.dp, badge.color.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = badge.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = badge.color,
        )
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun RowScope.BreakdownSegment(
    fraction: Float,
    color: Color,
    label: String,
    contentColor: Color,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = Modifier
            .weight(fraction)
            .fillMaxSize()
            .background(color = color, shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color = color, shape = CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape,
            )
        ) { content() }
    }
}

@Composable
private fun IncomeAmountDisplay(currency: String, value: String) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (value.isBlank()) "$currency 0.00" else "$currency $value",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun IncomeKeypad(value: String, onValueChange: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(96.dp, 56.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(16.dp),
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                when (key) {
                                    "⌫" -> onValueChange(value.dropLast(1))
                                    "." -> if (!value.contains(".")) onValueChange("$value.")
                                    else -> onValueChange(value + key)
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

// ── Data helpers ──────────────────────────────────────────────────────────────

private data class SourceOption(val key: String, val label: String, val icon: ImageVector)
private data class IncomeBadge(val label: String, val color: Color)
private data class IncomeEntry(
    val title: String,
    val primaryAmount: String,
    val secondaryAmount: String,
    val dateLabel: String,
    val icon: ImageVector,
    val tint: Color,
    val badges: List<IncomeBadge> = emptyList(),
)

@Composable
private fun Income.toIncomeEntry(): IncomeEntry {
    val label = sourceLabelFor(sourceType, sourceLabel)
    val badges = buildList<IncomeBadge> {
        if (isRecurring) add(IncomeBadge("recurring", MaterialTheme.colorScheme.primary))
        if (sourceType.uppercase() == "FREELANCE" && !invoicePaid) {
            add(IncomeBadge("pending", MaterialTheme.colorScheme.error))
        }
    }
    return IncomeEntry(
        title = if (!projectRef.isNullOrBlank()) "$label · $projectRef" else label,
        primaryAmount = "${formatAmount(amount)} $currency",
        secondaryAmount = "LKR ${formatAmount(amountLKR)}",
        dateLabel = formatDate(date),
        icon = sourceIconFor(sourceType),
        tint = sourceTintFor(sourceType),
        badges = badges,
    )
}

private fun formatAmount(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).format(amount)

private fun formatAmountInput(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM dd"))

private fun formatDateLong(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

private fun sourceIconFor(sourceType: String): ImageVector = when (sourceType.uppercase()) {
    "SALARY" -> Icons.Outlined.Work
    "FREELANCE" -> Icons.Outlined.BusinessCenter
    "ADSENSE" -> Icons.Outlined.Campaign
    "CRYPTO" -> Icons.Outlined.CurrencyBitcoin
    else -> Icons.Outlined.MoreHoriz
}

private fun sourceLabelFor(sourceType: String, customLabel: String?): String =
    when (sourceType.uppercase()) {
        "CUSTOM" -> customLabel?.takeIf { it.isNotBlank() } ?: "Custom"
        else -> sourceType.lowercase().replaceFirstChar { it.uppercase() }
    }

@Composable
private fun sourceTintFor(sourceType: String): Color = when (sourceType.uppercase()) {
    "SALARY" -> MaterialTheme.colorScheme.secondary
    "FREELANCE" -> MaterialTheme.colorScheme.primary
    "ADSENSE" -> MaterialTheme.colorScheme.primaryContainer
    "CRYPTO" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.errorContainer
}

private fun periodTitle(period: IncomePeriod): String = when (period) {
    IncomePeriod.WEEK -> "This Week"
    IncomePeriod.MONTH -> "This Month"
    IncomePeriod.YEAR -> "This Year"
}
