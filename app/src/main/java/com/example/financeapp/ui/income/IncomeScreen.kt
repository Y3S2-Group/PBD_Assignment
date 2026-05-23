package com.example.financeapp.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun IncomeScreen(viewModel: IncomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val totalLkr by viewModel.totalLkr.collectAsState()
    val sourceBreakdown by viewModel.sourceBreakdown.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }
    var deleteTarget by remember { mutableStateOf<Income?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadIncomeHistory()
    }

    val incomes = (state as? IncomeUiState.Success)?.entries.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                IncomeHeader(
                    totalLkr = totalLkr,
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = viewModel::setPeriod
                )
            }
            item { SourceBreakdownChart(breakdown = sourceBreakdown, totalLkr = totalLkr) }
            item {
                IncomeHistoryList(
                    incomes = incomes,
                    onEdit = { income ->
                        editingIncome = income
                        showBottomSheet = true
                    },
                    onDelete = { income ->
                        deleteTarget = income
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = {
                editingIncome = null
                showBottomSheet = true
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
                .size(64.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }

        if (showBottomSheet) {
            AddIncomeBottomSheet(
                onDismiss = {
                    showBottomSheet = false
                    editingIncome = null
                },
                initialIncome = editingIncome,
                onSave = { amount, currency, source, sourceLabel, notes ->
                    viewModel.addIncome(amount, currency, source, sourceLabel, notes)
                    showBottomSheet = false
                    editingIncome = null
                },
                onUpdate = { id, amount, currency, source, sourceLabel, date ->
                    viewModel.updateIncome(id, amount, currency, source, sourceLabel, date)
                    showBottomSheet = false
                    editingIncome = null
                }
            )
        }

        if (deleteTarget != null) {
            DeleteIncomeDialog(
                income = deleteTarget!!,
                onConfirm = {
                    viewModel.deleteIncome(deleteTarget!!.id)
                    deleteTarget = null
                },
                onDismiss = { deleteTarget = null }
            )
        }
    }
}

@Composable
private fun DeleteIncomeDialog(
    income: Income,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete income?") },
        text = { Text(text = "This will permanently remove ${sourceLabelFor(income.sourceType, income.sourceLabel)}.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun IncomeHeader(
    totalLkr: Double,
    selectedPeriod: IncomePeriod,
    onPeriodChange: (IncomePeriod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Vault",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Revenue Streams",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = periodTitle(selectedPeriod),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "LKR ${formatAmount(totalLkr)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Updated just now",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodChip(
                label = "Week",
                selected = selectedPeriod == IncomePeriod.WEEK,
                onClick = { onPeriodChange(IncomePeriod.WEEK) }
            )
            PeriodChip(
                label = "Month",
                selected = selectedPeriod == IncomePeriod.MONTH,
                onClick = { onPeriodChange(IncomePeriod.MONTH) }
            )
            PeriodChip(
                label = "Year",
                selected = selectedPeriod == IncomePeriod.YEAR,
                onClick = { onPeriodChange(IncomePeriod.YEAR) }
            )
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun SourceBreakdownChart(
    breakdown: List<SourceBreakdown>,
    totalLkr: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            if (breakdown.isEmpty() || totalLkr <= 0.0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No income data yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    breakdown.forEachIndexed { index, item ->
                        val fraction = (item.amountLkr / totalLkr).toFloat().coerceIn(0f, 1f)
                        val percentLabel = "${(fraction * 100).roundToInt()}%"
                        val shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            breakdown.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                        BreakdownSegment(
                            fraction = fraction,
                            color = sourceTintFor(item.sourceType),
                            label = percentLabel,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = shape
                        )
                    }
                }
            }
        }

        if (breakdown.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { item ->
                            LegendItem(
                                label = sourceLabelFor(item.sourceType, item.label),
                                color = sourceTintFor(item.sourceType)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeHistoryList(
    incomes: List<Income>,
    onEdit: (Income) -> Unit,
    onDelete: (Income) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Income",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = {}) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            incomes.forEach { income ->
                IncomeListItem(
                    income = income,
                    onEdit = { onEdit(income) },
                    onDelete = { onDelete(income) }
                )
            }
        }
    }
}

@Composable
private fun IncomeListItem(
    income: Income,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val entry = income.toIncomeEntry()
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = entry.tint.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = entry.tint.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = entry.title,
                        tint = entry.tint
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        entry.status?.let { status ->
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = status)
                        }
                    }
                    Text(
                        text = entry.secondaryAmount,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.primaryAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit income",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onEdit() }
                    )
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete income",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: IncomeStatus) {
    Box(
        modifier = Modifier
            .background(
                color = status.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = status.color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = status.color
        )
    }
}

@Composable
private fun RowScope.BreakdownSegment(
    fraction: Float,
    color: Color,
    label: String,
    contentColor: Color,
    shape: RoundedCornerShape = RoundedCornerShape(0.dp)
) {
    Box(
        modifier = Modifier
            .weight(fraction)
            .fillMaxSize()
            .background(color = color, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = shape
                )
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIncomeBottomSheet(
    onDismiss: () -> Unit,
    initialIncome: Income?,
    onSave: (Double, String, String, String?, String?) -> Unit,
    onUpdate: (String, Double, String, String, String?, Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCurrency by rememberSaveable { mutableStateOf("LKR") }
    var selectedSource by rememberSaveable { mutableStateOf("SALARY") }
    var customSourceLabel by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(initialIncome?.id) {
        if (initialIncome != null) {
            amountText = formatAmountInput(initialIncome.amount)
            selectedCurrency = initialIncome.currency
            selectedSource = initialIncome.sourceType.uppercase()
            customSourceLabel = initialIncome.sourceLabel.orEmpty()
        } else {
            amountText = ""
            selectedCurrency = "LKR"
            selectedSource = "SALARY"
            customSourceLabel = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialIncome == null) "Add Income" else "Edit Income",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(4.dp)
                        .clickable { onDismiss() }
                )
            }

            IncomeAmountDisplay(currency = selectedCurrency, value = amountText)
            IncomeKeypad(
                value = amountText,
                onValueChange = { amountText = it }
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LKR", "USD", "USDT", "ETH").forEach { currency ->
                        FilterChip(
                            selected = selectedCurrency == currency,
                            onClick = { selectedCurrency = currency },
                            label = { Text(currency) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Source",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sources = listOf(
                        SourceOption("SALARY", "Salary", Icons.Outlined.Work),
                        SourceOption("FREELANCE", "Freelance", Icons.Outlined.BusinessCenter),
                        SourceOption("ADSENSE", "AdSense", Icons.Outlined.Campaign),
                        SourceOption("CRYPTO", "Crypto", Icons.Outlined.CurrencyBitcoin),
                        SourceOption("CUSTOM", "Custom", Icons.Outlined.MoreHoriz)
                    )

                    sources.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { option ->
                                FilterChip(
                                    selected = selectedSource == option.key,
                                    onClick = { selectedSource = option.key },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.label
                                        )
                                    },
                                    label = { Text(option.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (selectedSource == "CUSTOM") {
                TextField(
                    value = customSourceLabel,
                    onValueChange = { customSourceLabel = it },
                    placeholder = { Text("Custom source name") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Add a note (optional)") },
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            val amountValue = amountText.toDoubleOrNull()
            val hasCustomLabel = selectedSource != "CUSTOM" || customSourceLabel.isNotBlank()
            val canSubmit = amountValue != null && amountValue > 0 && hasCustomLabel
            Button(
                onClick = {
                    if (canSubmit) {
                        val label = if (selectedSource == "CUSTOM") customSourceLabel.trim() else null
                        if (initialIncome == null) {
                            onSave(amountValue!!, selectedCurrency, selectedSource, label, notes.ifBlank { null })
                        } else {
                            onUpdate(
                                initialIncome.id,
                                amountValue!!,
                                selectedCurrency,
                                selectedSource,
                                label,
                                initialIncome.date
                            )
                        }
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (initialIncome == null) "Save Income" else "Update Income",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun IncomeAmountDisplay(
    currency: String,
    value: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (value.isBlank()) "$currency 0.00" else "$currency $value",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun IncomeKeypad(
    value: String,
    onValueChange: (String) -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowKeys.forEach { key ->
                    IncomeKeypadButton(
                        label = key,
                        onClick = {
                            when (key) {
                                "⌫" -> onValueChange(value.dropLast(1))
                                "." -> if (!value.contains(".")) onValueChange(value + ".")
                                else -> onValueChange(value + key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeKeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp, 56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class SourceOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

private data class IncomeEntry(
    val title: String,
    val primaryAmount: String,
    val secondaryAmount: String,
    val dateLabel: String,
    val icon: ImageVector,
    val tint: Color,
    val status: IncomeStatus? = null
)

private data class IncomeStatus(val label: String, val color: Color)

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(amount)
}

@Composable
private fun Income.toIncomeEntry(): IncomeEntry {
    val label = sourceLabelFor(sourceType, sourceLabel)
    return IncomeEntry(
        title = label,
        primaryAmount = "${formatAmount(amount)} $currency",
        secondaryAmount = "LKR ${formatAmount(amountLKR)}",
        dateLabel = formatDate(date),
        icon = sourceIconFor(sourceType),
        tint = sourceTintFor(sourceType)
    )
}

private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

private fun sourceIconFor(sourceType: String): ImageVector {
    return when (sourceType.uppercase()) {
        "SALARY" -> Icons.Outlined.Work
        "FREELANCE" -> Icons.Outlined.BusinessCenter
        "ADSENSE" -> Icons.Outlined.Campaign
        "CRYPTO" -> Icons.Outlined.CurrencyBitcoin
        "CUSTOM" -> Icons.Outlined.MoreHoriz
        else -> Icons.Outlined.Work
    }
}

private fun sourceLabelFor(sourceType: String, customLabel: String?): String {
    return when (sourceType.uppercase()) {
        "CUSTOM" -> customLabel?.takeIf { it.isNotBlank() } ?: "Custom"
        else -> sourceType.lowercase().replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun sourceTintFor(sourceType: String): Color {
    return when (sourceType.uppercase()) {
        "SALARY" -> MaterialTheme.colorScheme.secondary
        "FREELANCE" -> MaterialTheme.colorScheme.primary
        "ADSENSE" -> MaterialTheme.colorScheme.primaryContainer
        "CRYPTO" -> MaterialTheme.colorScheme.tertiary
        "CUSTOM" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondary
    }
}

private fun periodTitle(period: IncomePeriod): String {
    return when (period) {
        IncomePeriod.WEEK -> "This Week"
        IncomePeriod.MONTH -> "This Month"
        IncomePeriod.YEAR -> "This Year"
    }
}

private fun formatAmountInput(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        amount.toString()
    }
}
