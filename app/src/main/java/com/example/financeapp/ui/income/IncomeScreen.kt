package com.example.financeapp.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun IncomeScreen(viewModel: IncomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val totalLkr by viewModel.totalLkr.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadIncomeHistory()
    }

    val incomeEntries = (state as? IncomeUiState.Success)
        ?.entries
        ?.map { it.toIncomeEntry() }
        .orEmpty()

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
            item { IncomeHeader(totalLkr = totalLkr) }
            item { SourceBreakdownChart() }
            item { IncomeHistoryList(entries = incomeEntries) }
        }

        FloatingActionButton(
            onClick = { showBottomSheet = true },
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
                onDismiss = { showBottomSheet = false },
                onSave = { amount, currency, source, notes ->
                    viewModel.addIncome(amount, currency, source, notes)
                    showBottomSheet = false
                }
            )
        }
    }
}

@Composable
private fun IncomeHeader(totalLkr: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                    text = "This Month",
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
    }
}

@Composable
private fun SourceBreakdownChart() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                BreakdownSegment(
                    fraction = 0.703f,
                    color = MaterialTheme.colorScheme.secondary,
                    label = "70%",
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
                BreakdownSegment(
                    fraction = 0.145f,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    label = "15%",
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                BreakdownSegment(
                    fraction = 0.084f,
                    color = MaterialTheme.colorScheme.primary,
                    label = "8%",
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                BreakdownSegment(
                    fraction = 0.068f,
                    color = MaterialTheme.colorScheme.tertiary,
                    label = "7%",
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(label = "Salary", color = MaterialTheme.colorScheme.secondary)
                LegendItem(label = "Freelance", color = MaterialTheme.colorScheme.primaryContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(label = "AdSense", color = MaterialTheme.colorScheme.primary)
                LegendItem(label = "Crypto", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun IncomeHistoryList(entries: List<IncomeEntry>) {
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
            entries.forEach { entry ->
                IncomeListItem(entry = entry)
            }
        }
    }
}

@Composable
private fun IncomeListItem(entry: IncomeEntry) {
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
    onSave: (Double, String, String, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCurrency by rememberSaveable { mutableStateOf("LKR") }
    var selectedSource by rememberSaveable { mutableStateOf("SALARY") }
    var notes by rememberSaveable { mutableStateOf("") }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedCurrency,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    TextField(
                        value = amountText,
                        onValueChange = { value ->
                            amountText = value.filter { it.isDigit() || it == '.' }
                        },
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        placeholder = {
                            Text(
                                text = "0.00",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.width(180.dp)
                    )
                }
            }

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
                        SourceOption("CRYPTO", "Crypto", Icons.Outlined.CurrencyBitcoin)
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
            Button(
                onClick = {
                    if (amountValue != null) {
                        onSave(amountValue, selectedCurrency, selectedSource, notes.ifBlank { null })
                    }
                },
                enabled = amountValue != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Save Income", style = MaterialTheme.typography.titleMedium)
            }
        }
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
    return IncomeEntry(
        title = sourceType.replaceFirstChar { it.uppercase() },
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
        else -> Icons.Outlined.Work
    }
}

@Composable
private fun sourceTintFor(sourceType: String): Color {
    return when (sourceType.uppercase()) {
        "SALARY" -> MaterialTheme.colorScheme.secondary
        "FREELANCE" -> MaterialTheme.colorScheme.primary
        "ADSENSE" -> MaterialTheme.colorScheme.primaryContainer
        "CRYPTO" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
}
