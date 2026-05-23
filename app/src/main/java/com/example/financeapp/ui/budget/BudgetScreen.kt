package com.example.financeapp.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Commute
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.BudgetCategorySummary
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults

@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    val goal = state.activeGoal
    val progress = (state.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "goalProgress"
    )

    var diningReduction by rememberSaveable { mutableStateOf(5000f) }
    var entertainmentReduction by rememberSaveable { mutableStateOf(2000f) }
    var showBoostDialog by rememberSaveable { mutableStateOf(false) }
    var boostAmountInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        BudgetTopBar()
        if (goal != null) {
            SavingsGoalCard(
                goal = goal,
                progress = animatedProgress,
                progressPercent = state.progressPercent,
                requiredMonthlySavings = state.requiredMonthlySavings,
                onBoostClick = { showBoostDialog = true }
            )
        } else {
            EmptyGoalCard()
        }
        MonthlyBudgetSection(categories = state.categoryBudgets)
        BudgetOptimizerCard(
            diningReduction = diningReduction,
            entertainmentReduction = entertainmentReduction,
            onDiningChange = { diningReduction = it },
            onEntertainmentChange = { entertainmentReduction = it }
        )
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
}

@Composable
private fun BudgetTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Vault",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(
    goal: Goal,
    progress: Float,
    progressPercent: Double,
    requiredMonthlySavings: Double,
    onBoostClick: () -> Unit
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
                    Column {
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
                    GoalStatusChip(text = "On Track")
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
                            modifier = Modifier.matchParentSize().padding(8.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated Finish",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDeadline(goal.deadlineTimestamp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Button(
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
}

@Composable
private fun GoalStatusChip(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun MonthlyBudgetSection(categories: List<BudgetCategorySummary>) {
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
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (categories.isEmpty()) {
            GlassCard {
                Text(
                    text = "No budgets set for this month yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.forEach { category ->
                    BudgetCategoryCard(category)
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryCard(category: BudgetCategorySummary) {
    val visuals = categoryVisuals(category.categoryName)
    val progress = if (category.allocatedAmount > 0.0) {
        (category.actualSpent / category.allocatedAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val accent = if (category.actualSpent > category.allocatedAmount) {
        MaterialTheme.colorScheme.error
    } else {
        visuals.accent
    }

    GlassCard {
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
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = null,
                    tint = accent
                )
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
                    Text(
                        text = "${formatLkr(category.actualSpent)} / ${formatLkr(category.allocatedAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressTrack(
                    progress = progress,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun BudgetOptimizerCard(
    diningReduction: Float,
    entertainmentReduction: Float,
    onDiningChange: (Float) -> Unit,
    onEntertainmentChange: (Float) -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = "Budget Optimizer", style = MaterialTheme.typography.titleMedium)
            }

            BudgetSliderRow(
                label = "Dining Out",
                value = diningReduction,
                onValueChange = onDiningChange
            )
            BudgetSliderRow(
                label = "Entertainment",
                value = entertainmentReduction,
                onValueChange = onEntertainmentChange
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = "If you reduce Dining by ${formatLkr(diningReduction.toDouble())}, you reach your MacBook goal 2 months earlier.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun BudgetSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "- ${formatLkr(value.toDouble())}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..10_000f,
            steps = 19
        )
    }
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
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
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

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = stroke
        )
        drawArc(
            brush = Brush.sweepGradient(colors = gradientColors),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = stroke
        )
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

private fun formatLkr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "LKR ${formatter.format(amount)}"
}

private fun formatDeadline(deadlineTimestamp: Long): String {
    if (deadlineTimestamp <= 0L) return "October 2024"
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    return Instant.ofEpochMilli(deadlineTimestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

@Composable
private fun EmptyGoalCard() {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Savings Goal",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "No goal available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        title = { Text(text = "Boost Savings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter the amount you want to add toward your goal.",
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
            TextButton(onClick = onConfirm) {
                Text(text = "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

private data class BudgetCategoryVisual(
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun categoryVisuals(categoryName: String): BudgetCategoryVisual {
    return when (categoryName.lowercase(Locale.US)) {
        "food", "food & dining", "dining" -> BudgetCategoryVisual(
            icon = Icons.Rounded.Restaurant,
            accent = MaterialTheme.colorScheme.secondary
        )
        "tech", "tech & gadgets", "gadgets" -> BudgetCategoryVisual(
            icon = Icons.Rounded.Devices,
            accent = MaterialTheme.colorScheme.tertiary
        )
        "subscriptions", "subscription" -> BudgetCategoryVisual(
            icon = Icons.Rounded.Subscriptions,
            accent = MaterialTheme.colorScheme.error
        )
        "transport", "commute" -> BudgetCategoryVisual(
            icon = Icons.Rounded.Commute,
            accent = MaterialTheme.colorScheme.secondary
        )
        else -> BudgetCategoryVisual(
            icon = Icons.Rounded.AccountBalanceWallet,
            accent = MaterialTheme.colorScheme.primary
        )
    }
}
