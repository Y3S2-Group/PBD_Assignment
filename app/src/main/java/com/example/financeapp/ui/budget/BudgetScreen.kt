package com.example.financeapp.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Commute
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.domain.model.BudgetCategorySummary
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.ui.components.GlobalTopAppBar
import com.example.financeapp.ui.dashboard.DashboardViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BUDGET_CATEGORIES = listOf(
    "Coffee", "Food", "Transport", "Shop", "Subs", "Utility", "Commute", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    avatarId: String,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dashVm: DashboardViewModel = hiltViewModel()
    val dashState by dashVm.state.collectAsState()
    val scrollState = rememberScrollState()

    val goal = state.activeGoal
    val progress = (state.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "goalProgress"
    )

    val topSpenders = state.categoryBudgets.filter { it.actualSpent > 0 }.sortedByDescending { it.actualSpent }
    val cat1 = topSpenders.getOrNull(0)
    val cat2 = topSpenders.getOrNull(1)
    val cat1Name = cat1?.categoryName ?: "Category 1"
    val cat2Name = cat2?.categoryName ?: "Category 2"
    val cat1Max = maxOf((cat1?.actualSpent ?: 5_000.0).toFloat(), 1_000f)
    val cat2Max = maxOf((cat2?.actualSpent ?: 3_000.0).toFloat(), 1_000f)

    var cat1Reduction by rememberSaveable { mutableStateOf(0f) }
    var cat2Reduction by rememberSaveable { mutableStateOf(0f) }
    var showBoostDialog by rememberSaveable { mutableStateOf(false) }
    var boostAmountInput by rememberSaveable { mutableStateOf("") }
    var showCreateGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showEditGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showSetBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var editingCategory by rememberSaveable { mutableStateOf("") }
    var editingCategoryAmount by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        GlobalTopAppBar(
            title = "Budget",
            subtitle = null,
            healthScore = dashState.healthScore,
            avatarId = avatarId,
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick,
        )

        if (goal != null) {
            SavingsGoalCard(
                goal = goal,
                progress = animatedProgress,
                progressPercent = state.progressPercent,
                requiredMonthlySavings = state.requiredMonthlySavings,
                goalStatus = state.goalStatus,
                daysRemaining = state.daysRemaining,
                projectedCompletionDate = state.projectedCompletionDate,
                onBoostClick = { showBoostDialog = true },
                onEditClick = { showEditGoalDialog = true }
            )
        } else {
            EmptyGoalCard(onCreateGoal = { showCreateGoalDialog = true })
        }

        if (state.savingsStreak > 0) {
            SavingsStreakCard(streak = state.savingsStreak)
        }

        MonthlyBudgetSection(
            categories = state.categoryBudgets,
            onAddCategory = {
                editingCategory = ""
                editingCategoryAmount = ""
                showSetBudgetDialog = true
            },
            onEditCategory = { cat ->
                editingCategory = cat.categoryName
                editingCategoryAmount = cat.allocatedAmount.toInt().toString()
                showSetBudgetDialog = true
            }
        )

        if (goal != null && (cat1 != null || cat2 != null)) {
            BudgetOptimizerCard(
                goal = goal,
                cat1Name = cat1Name,
                cat1Max = cat1Max,
                cat1ActualSpend = cat1?.actualSpent ?: 0.0,
                cat2Name = cat2Name,
                cat2Max = cat2Max,
                cat2ActualSpend = cat2?.actualSpent ?: 0.0,
                cat1Reduction = cat1Reduction,
                cat2Reduction = cat2Reduction,
                onCat1Change = { cat1Reduction = it },
                onCat2Change = { cat2Reduction = it },
                monthlySavingsBoost = (cat1Reduction + cat2Reduction).toDouble(),
                monthsEarlier = viewModel.calculateMonthsEarlier(
                    (cat1Reduction + cat2Reduction).toDouble(),
                    goal
                )
            )
        }
    }

    if (showBoostDialog) {
        BoostSavingsDialog(
            amountInput = boostAmountInput,
            onAmountChange = { boostAmountInput = it },
            onDismiss = { showBoostDialog = false },
            onConfirm = {
                val amount = boostAmountInput.toDoubleOrNull() ?: 0.0
                viewModel.addSavingsToGoal(amount)
                boostAmountInput = ""
                showBoostDialog = false
            }
        )
    }

    if (showCreateGoalDialog) {
        GoalFormDialog(
            title = "Create Savings Goal",
            initialName = "",
            initialAmount = "",
            initialCurrency = "LKR",
            initialDeadlineMs = Instant.now().plusSeconds(365L * 24 * 60 * 60).toEpochMilli(),
            onDismiss = { showCreateGoalDialog = false },
            onConfirm = { name, amount, currency, deadline ->
                viewModel.createGoal(name, amount, currency, deadline)
                showCreateGoalDialog = false
            }
        )
    }

    if (showEditGoalDialog && goal != null) {
        val displayAmount = if (goal.currency == "USD") goal.targetAmount / 300.0 else goal.targetAmount
        GoalFormDialog(
            title = "Edit Savings Goal",
            initialName = goal.name,
            initialAmount = displayAmount.toInt().toString(),
            initialCurrency = goal.currency,
            initialDeadlineMs = goal.deadlineTimestamp,
            onDismiss = { showEditGoalDialog = false },
            onConfirm = { name, amount, currency, deadline ->
                viewModel.updateGoal(name, amount, currency, deadline)
                showEditGoalDialog = false
            }
        )
    }

    if (showSetBudgetDialog) {
        SetCategoryBudgetDialog(
            initialCategory = editingCategory,
            initialAmount = editingCategoryAmount,
            onDismiss = { showSetBudgetDialog = false },
            onSave = { category, amount ->
                viewModel.setCategoryBudget(category, amount)
                showSetBudgetDialog = false
            }
        )
    }
}


@Composable
private fun SavingsGoalCard(
    goal: Goal,
    progress: Float,
    progressPercent: Double,
    requiredMonthlySavings: Double,
    goalStatus: GoalStatus,
    daysRemaining: Int,
    projectedCompletionDate: Long,
    onBoostClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val gradient = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, brush = gradient, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(1.dp)
    ) {
        GlassCard(shape = shape, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Savings Goal",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GoalStatusChip(status = goalStatus)
                        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Goal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedProgressRing(
                            progress = progress,
                            modifier = Modifier
                                .matchParentSize()
                                .padding(8.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatLkr(goal.currentSavings)} / ${formatLkr(goal.targetAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${progressPercent.toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        GradientProgressBar(progress = progress)
                        Text(
                            text = "Required: ${formatLkr(requiredMonthlySavings)} / mo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Deadline",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDeadline(goal.deadlineTimestamp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$daysRemaining days left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Projected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (projectedCompletionDate > 0L) formatDeadline(projectedCompletionDate)
                            else "—",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBoostClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "Boost Savings")
                }
            }
        }
    }
}

@Composable
private fun GoalStatusChip(status: GoalStatus) {
    val (label, color) = when (status) {
        GoalStatus.AHEAD -> "Ahead" to MaterialTheme.colorScheme.tertiary
        GoalStatus.ON_TRACK -> "On Track" to MaterialTheme.colorScheme.secondary
        GoalStatus.BEHIND -> "Behind" to MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun SavingsStreakCard(streak: Int) {
    GlassCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (streak >= 3) Icons.Rounded.EmojiEvents else Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = "$streak Month${if (streak != 1) "s" else ""} Streak",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (streak >= 3) "Outstanding! Keep the momentum going."
                    else "You're building great savings habits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlyBudgetSection(
    categories: List<BudgetCategorySummary>,
    onAddCategory: () -> Unit,
    onEditCategory: (BudgetCategorySummary) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Budget",
                style = MaterialTheme.typography.titleLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onAddCategory),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Budget",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (categories.isEmpty()) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "No budgets set for this month yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onAddCategory) {
                        Text("Set a budget category")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.forEach { category ->
                    BudgetCategoryCard(
                        category = category,
                        onClick = { onEditCategory(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryCard(category: BudgetCategorySummary, onClick: () -> Unit) {
    val visuals = categoryVisuals(category.categoryName)
    val isBudgeted = category.allocatedAmount > 0.0
    val progress = if (isBudgeted) {
        (category.actualSpent / category.allocatedAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val accent = when {
        !isBudgeted -> MaterialTheme.colorScheme.onSurfaceVariant
        category.actualSpent > category.allocatedAmount -> MaterialTheme.colorScheme.error
        else -> visuals.accent
    }

    GlassCard(
        modifier = Modifier.clickable(onClick = onClick),
        containerColor = if (!isBudgeted)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = visuals.icon, contentDescription = null, tint = accent)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = category.categoryName, style = MaterialTheme.typography.titleMedium)
                    if (isBudgeted) {
                        Text(
                            text = "${formatLkr(category.actualSpent)} / ${formatLkr(category.allocatedAmount)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Spent: ${formatLkr(category.actualSpent)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isBudgeted) {
                    LinearProgressTrack(progress = progress, accent = accent)
                } else {
                    Text(
                        text = "Tap to set a budget",
                        style = MaterialTheme.typography.labelSmall.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetOptimizerCard(
    goal: Goal,
    cat1Name: String,
    cat1Max: Float,
    cat1ActualSpend: Double,
    cat2Name: String,
    cat2Max: Float,
    cat2ActualSpend: Double,
    cat1Reduction: Float,
    cat2Reduction: Float,
    onCat1Change: (Float) -> Unit,
    onCat2Change: (Float) -> Unit,
    monthlySavingsBoost: Double,
    monthsEarlier: Int
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = "Budget Optimizer", style = MaterialTheme.typography.titleMedium)
            }

            OptimizerSliderRow(
                label = cat1Name,
                value = cat1Reduction,
                valueRange = 0f..cat1Max,
                actualSpend = cat1ActualSpend,
                onValueChange = onCat1Change
            )
            OptimizerSliderRow(
                label = cat2Name,
                value = cat2Reduction,
                valueRange = 0f..cat2Max,
                actualSpend = cat2ActualSpend,
                onValueChange = onCat2Change
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = if (monthsEarlier > 0) {
                        "Saving ${formatLkr(monthlySavingsBoost)} more/month by reducing $cat1Name and " +
                            "$cat2Name spending helps you reach ${goal.name} " +
                            "$monthsEarlier month${if (monthsEarlier != 1) "s" else ""} earlier."
                    } else {
                        "Adjust the sliders to see how spending cuts accelerate your ${goal.name} goal."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun OptimizerSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    actualSpend: Double,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Spent this month: ${formatLkr(actualSpend)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "- ${formatLkr(value.toDouble())}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = ((valueRange.endInclusive / 500f).toInt() - 1).coerceAtLeast(0)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalFormDialog(
    title: String,
    initialName: String,
    initialAmount: String,
    initialCurrency: String,
    initialDeadlineMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, currency: String, deadlineMs: Long) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var currency by rememberSaveable { mutableStateOf(initialCurrency) }
    var deadlineMs by rememberSaveable { mutableStateOf(initialDeadlineMs) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var currencyExpanded by rememberSaveable { mutableStateOf(false) }

    val isValid = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g., MacBook Pro M4") },
                    singleLine = true
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Target Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        modifier = Modifier.width(90.dp),
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            listOf("LKR", "USD").forEach { cur ->
                                DropdownMenuItem(
                                    text = { Text(cur) },
                                    onClick = {
                                        currency = cur
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text(text = "Deadline: ${formatDeadline(deadlineMs)}")
                }
                if (currency == "USD") {
                    val lkrEquivalent = (amount.toDoubleOrNull() ?: 0.0) * 300.0
                    Text(
                        text = "≈ ${formatLkr(lkrEquivalent)} (at 300 LKR/USD)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, amount.toDouble(), currency, deadlineMs) },
                enabled = isValid
            ) { Text(if (initialName.isBlank()) "Create" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = deadlineMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineMs = datePickerState.selectedDateMillis ?: deadlineMs
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetCategoryBudgetDialog(
    initialCategory: String,
    initialAmount: String,
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double) -> Unit
) {
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }

    val isEditing = initialCategory.isNotBlank()
    val isValid = category.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Budget" else "Add Budget Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditing) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            value = category.ifBlank { "Select category" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            BUDGET_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Allocated Amount (LKR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(category, amount.toDoubleOrNull() ?: 0.0) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun AnimatedProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
    )
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 8f, cap = StrokeCap.Round)
        val diameter = size.minDimension
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val ringSize = Size(diameter, diameter)
        drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = ringSize, style = stroke)
        drawArc(brush = Brush.sweepGradient(colors = gradientColors), startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, topLeft = topLeft, size = ringSize, style = stroke)
    }
}

@Composable
private fun GradientProgressBar(progress: Float) {
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(progress)
                .background(gradient)
        )
    }
}

@Composable
private fun LinearProgressTrack(progress: Float, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth(progress)
                .background(accent)
        )
    }
}

@Composable
private fun EmptyGoalCard(onCreateGoal: () -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Savings Goal", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Set up your first savings goal to start tracking progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCreateGoal,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Goal")
            }
        }
    }
}

@Composable
private fun BoostSavingsDialog(
    amountInput: String,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Log Savings Deposit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter the amount you transferred to savings.",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextField(
                    value = amountInput,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = (amountInput.toDoubleOrNull() ?: 0.0) > 0
            ) { Text(text = "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        }
    )
}

private data class BudgetCategoryVisual(val icon: ImageVector, val accent: Color)

@Composable
private fun categoryVisuals(categoryName: String): BudgetCategoryVisual {
    return when (categoryName.lowercase(Locale.US)) {
        "coffee" -> BudgetCategoryVisual(Icons.Rounded.LocalCafe, MaterialTheme.colorScheme.tertiary)
        "food" -> BudgetCategoryVisual(Icons.Rounded.Restaurant, MaterialTheme.colorScheme.secondary)
        "transport" -> BudgetCategoryVisual(Icons.Rounded.DirectionsCar, MaterialTheme.colorScheme.primary)
        "shop" -> BudgetCategoryVisual(Icons.Rounded.ShoppingBag, MaterialTheme.colorScheme.secondary)
        "subs" -> BudgetCategoryVisual(Icons.Rounded.Subscriptions, MaterialTheme.colorScheme.error)
        "utility" -> BudgetCategoryVisual(Icons.Rounded.Bolt, MaterialTheme.colorScheme.tertiary)
        "commute" -> BudgetCategoryVisual(Icons.Rounded.Commute, MaterialTheme.colorScheme.primary)
        else -> BudgetCategoryVisual(Icons.Rounded.MoreHoriz, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatLkr(amount: Double): String =
    "LKR ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"

private fun formatDeadline(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
}
