package com.example.financeapp.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Commute
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.ui.components.GlobalTopAppBar
import com.example.financeapp.ui.dashboard.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@Composable
fun ExpenseScreen(
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: ExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dashVm: DashboardViewModel = hiltViewModel()
    val dashState by dashVm.state.collectAsState()
    var showQuickAdd by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        GlobalTopAppBar(
            title = "Expenses",
            subtitle = null,
            healthScore = dashState.healthScore,
            localProfilePhotoPath = null,
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item { ExpenseSummaryCard(totalSpent = state.expenses.sumOf { it.amountLkr }) }
            item {
                CategoryFilterRow(
                    selectedCategory = state.selectedFilterCategory,
                    onCategorySelected = viewModel::setFilterCategory
                )
            }
            item { ExpenseActivityList(expenses = state.filteredExpenses) }
        }
        }

        FloatingActionButton(
            onClick = { showQuickAdd = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp)
                .size(56.dp)
        ) {
            Text(text = "+", style = MaterialTheme.typography.headlineMedium)
        }

        if (showQuickAdd) {
            QuickAddOverlay(
                onDismiss = { showQuickAdd = false },
                onSave = { amount, category ->
                    val expense = Expense(
                        id = "exp_${UUID.randomUUID()}",
                        amountLkr = amount,
                        category = category,
                        spendingType = spendingTypeFor(category),
                        paymentMethod = "Card",
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.addExpense(expense)
                    showQuickAdd = false
                }
            )
        }
    }
}


@Composable
private fun ExpenseSummaryCard(totalSpent: Double) {
    GlassCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Total Spent This Month",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "LKR ${formatAmount(totalSpent)}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+5.2%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "from last month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterPill(
                label = "All",
                selected = selectedCategory.equals("All", ignoreCase = true),
                onClick = { onCategorySelected("All") }
            )
        }
        listOf("Food", "Transport", "Tech", "Subs", "Travel").forEach { label ->
            item {
                FilterPill(
                    label = label,
                    selected = selectedCategory.equals(label, ignoreCase = true),
                    onClick = { onCategorySelected(label) }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}

@Composable
private fun ExpenseActivityList(expenses: List<Expense>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View All",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (expenses.isEmpty()) {
            Text(
                text = "No expenses yet. Add one to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            expenses.take(5).forEach { expense ->
                ExpenseItem(expense)
            }
        }
    }
}

@Composable
private fun ExpenseItem(expense: Expense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForCategory(expense.category),
                    contentDescription = expense.category,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = expense.paymentMethod,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "-LKR ${formatAmount(expense.amountLkr)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun QuickAddOverlay(
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Food") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Expense",
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

            AmountDisplay(amountText)
            CategoryGrid(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            QuickAddKeypad(
                value = amountText,
                onValueChange = { amountText = it }
            )

            val amountValue = amountText.toDoubleOrNull()
            Button(
                onClick = {
                    if (amountValue != null && amountValue > 0) {
                        onSave(amountValue, selectedCategory)
                        amountText = ""
                    }
                },
                enabled = amountValue != null && amountValue > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Save Expense", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun AmountDisplay(value: String) {
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
                text = if (value.isBlank()) "LKR 0.00" else "LKR $value",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        CategoryOption("Coffee", Icons.Outlined.LocalCafe),
        CategoryOption("Food", Icons.Outlined.Restaurant),
        CategoryOption("Transport", Icons.Outlined.DirectionsCar),
        CategoryOption("Shop", Icons.Outlined.ShoppingBag),
        CategoryOption("Subs", Icons.Outlined.Subscriptions),
        CategoryOption("Utility", Icons.Outlined.Bolt),
        CategoryOption("Commute", Icons.Outlined.Commute),
        CategoryOption("Other", Icons.Outlined.MoreHoriz)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.chunked(4).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { option ->
                    CategoryIcon(
                        option = option,
                        selected = selectedCategory == option.label,
                        onClick = { onCategorySelected(option.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryIcon(
    option: CategoryOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(background, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                .clickable { onClick() }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = option.icon, contentDescription = option.label, tint = contentColor)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickAddKeypad(
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
                    KeypadButton(
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
private fun KeypadButton(
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

private data class CategoryOption(val label: String, val icon: ImageVector)

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(amount)
}

private fun iconForCategory(category: String): ImageVector {
    return when (category.lowercase()) {
        "coffee" -> Icons.Outlined.LocalCafe
        "food" -> Icons.Outlined.Restaurant
        "transport" -> Icons.Outlined.DirectionsCar
        "commute" -> Icons.Outlined.Commute
        "subs" -> Icons.Outlined.Subscriptions
        "shop" -> Icons.Outlined.ShoppingBag
        "utility" -> Icons.Outlined.Bolt
        else -> Icons.Outlined.Widgets
    }
}

private fun spendingTypeFor(category: String): String {
    return when (category.lowercase()) {
        "subs", "utility" -> "COMMITTED"
        else -> "DISCRETIONARY"
    }
}
