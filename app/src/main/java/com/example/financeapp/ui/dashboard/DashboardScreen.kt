package com.example.financeapp.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.domain.model.BreakdownSlice
import com.example.financeapp.domain.model.DashboardAnalytics
import com.example.financeapp.domain.model.DashboardInsight
import com.example.financeapp.domain.model.GoalSnapshot
import com.example.financeapp.domain.model.InsightTone
import com.example.financeapp.domain.model.SpendingBreakdown
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        DashboardUiState.Loading -> DashboardLoadingState()
        is DashboardUiState.Error -> DashboardErrorState(
            message = currentState.message,
            onRetry = viewModel::loadDashboard
        )
        is DashboardUiState.Success -> DashboardContent(summary = currentState.summary)
    }
}

@Composable
internal fun DashboardLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun DashboardErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Text(text = message, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onRetry) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun DashboardContent(summary: DashboardAnalytics) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp)
        ) {
            FinancialHealthHeader(score = summary.healthScore)
            Spacer(modifier = Modifier.height(24.dp))
            BalanceCards(summary)
            Spacer(modifier = Modifier.height(24.dp))
            GoalProgressWidget(goal = summary.goal)
            Spacer(modifier = Modifier.height(24.dp))
            DataVizSection(summary = summary)
            Spacer(modifier = Modifier.height(24.dp))
            TrendSummaryCard(
                summary = summary,
                onOpenReport = { openReport(context) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            InsightAlerts(insights = summary.insights)
        }

        FloatingActionButton(
            onClick = {},
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp)
                .size(56.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add")
        }
    }
}

@Composable
private fun FinancialHealthHeader(score: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
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
            Column {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Financial Mastery",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            HealthScoreBadge(score = score)
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(40.dp)
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
}

@Composable
private fun HealthScoreBadge(score: Int) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressRing(
            progress = score / 100f,
            strokeWidth = 4.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            progressColor = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BalanceCards(summary: DashboardAnalytics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "This Month Income",
            amount = formatLkr(summary.totalIncomeLkr),
            subtitle = "${summary.incomeBreakdown.size} active income source(s)",
            amountColor = MaterialTheme.colorScheme.secondary,
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            iconTint = MaterialTheme.colorScheme.secondary
        )
        MetricCard(
            title = "Expenses",
            amount = formatLkr(summary.totalExpenseLkr),
            subtitle = "${summary.categoryBreakdown.size} tracked category slice(s)",
            amountColor = MaterialTheme.colorScheme.onSurface,
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            iconTint = MaterialTheme.colorScheme.tertiary
        )
        MetricCard(
            title = "Net Savings",
            amount = formatLkr(summary.netSavingsLkr),
            subtitle = "${summary.savingsRate}% savings rate",
            amountColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Rounded.AccountBalanceWallet,
            iconTint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: String,
    subtitle: String,
    amountColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalProgressWidget(goal: GoalSnapshot) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(80.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(128.dp),
                contentAlignment = Alignment.Center
            ) {
                GradientProgressRing(progress = goal.progress)
                Text(
                    text = "${(goal.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Goal in Focus",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleLarge
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatLkr(goal.currentAmountLkr)} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatLkr(goal.targetAmountLkr)} target",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    GoalProgressBar(progress = goal.progress)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${goal.daysLeft} days left",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataVizSection(summary: DashboardAnalytics) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wideLayout = maxWidth > 700.dp
        if (wideLayout) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DonutBreakdownCard(
                    categoryBreakdown = summary.categoryBreakdown,
                    totalExpenses = summary.totalExpenseLkr,
                    modifier = Modifier.weight(1f)
                )
                BudgetTypeCard(
                    spendingBreakdown = summary.spendingBreakdown,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                DonutBreakdownCard(
                    categoryBreakdown = summary.categoryBreakdown,
                    totalExpenses = summary.totalExpenseLkr,
                    modifier = Modifier.fillMaxWidth()
                )
                BudgetTypeCard(
                    spendingBreakdown = summary.spendingBreakdown,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DonutBreakdownCard(
    categoryBreakdown: List<BreakdownSlice>,
    totalExpenses: Double,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Monthly Breakdown",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DonutChart(categoryBreakdown)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = formatLkr(totalExpenses),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                CategoryLegend(categoryBreakdown)
            }
        }
    }
}

@Composable
private fun DonutChart(categoryBreakdown: List<BreakdownSlice>) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.outline
    )
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .size(192.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12f, cap = StrokeCap.Butt)
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 8f

            if (categoryBreakdown.isEmpty()) {
                drawArc(
                    color = emptyColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = stroke
                )
            } else {
                var startAngle = -90f
                categoryBreakdown.forEachIndexed { index, slice ->
                    val sweep = (slice.percentage / 100f) * 360f
                    drawArc(
                        color = palette[index % palette.size],
                        startAngle = startAngle,
                        sweepAngle = sweep.toFloat(),
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = stroke
                    )
                    startAngle += sweep.toFloat()
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (categoryBreakdown.isEmpty()) "No data" else "Spent",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun CategoryLegend(categoryBreakdown: List<BreakdownSlice>) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.outline
    )

    if (categoryBreakdown.isEmpty()) {
        Text(
            text = "Expense categories will appear once transactions are tracked.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categoryBreakdown.forEachIndexed { index, slice ->
            LegendItem(
                label = "${slice.label} (${slice.percentage.toInt()}%)",
                color = palette[index % palette.size]
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
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
private fun BudgetTypeCard(
    spendingBreakdown: SpendingBreakdown,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Budget Type",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "How your monthly spend is allocated",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Discretionary (${spendingBreakdown.discretionaryPercentage.toInt()}%)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Committed (${spendingBreakdown.committedPercentage.toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(spendingBreakdown.discretionaryPercentage.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WANT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(spendingBreakdown.committedPercentage.toFloat().coerceAtLeast(1f))
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Committed spend: ${formatLkr(spendingBreakdown.committedAmountLkr)}. Discretionary spend: ${formatLkr(spendingBreakdown.discretionaryAmountLkr)}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendSummaryCard(
    summary: DashboardAnalytics,
    onOpenReport: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6-Month Trend",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onOpenReport) {
                    Text("View Report")
                }
            }
            if (summary.monthlyTrend.isEmpty()) {
                Text(
                    text = "Monthly trend data will appear after more activity is tracked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxAmount = summary.monthlyTrend.maxOf { maxOf(it.incomeLkr, it.expenseLkr, 1.0) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    summary.monthlyTrend.forEach { point ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight((point.incomeLkr / maxAmount).toFloat())
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(MaterialTheme.colorScheme.secondary)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight((point.expenseLkr / maxAmount).toFloat())
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = "Green bars show income. Coral bars show expenses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightAlerts(insights: List<DashboardInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alerts & Insights",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Live",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        insights.forEach { insight ->
            val icon = when (insight.tone) {
                InsightTone.Positive -> Icons.Rounded.CheckCircle
                InsightTone.Warning -> Icons.Rounded.Warning
                InsightTone.Neutral -> Icons.Rounded.Info
            }
            val tint = when (insight.tone) {
                InsightTone.Positive -> MaterialTheme.colorScheme.secondary
                InsightTone.Warning -> MaterialTheme.colorScheme.error
                InsightTone.Neutral -> MaterialTheme.colorScheme.primary
            }
            val background = when (insight.tone) {
                InsightTone.Positive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                InsightTone.Warning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                InsightTone.Neutral -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
            AlertItem(
                icon = icon,
                iconTint = tint,
                iconBackground = background,
                title = insight.title,
                subtitle = insight.message,
                actionText = when (insight.tone) {
                    InsightTone.Positive -> "Healthy"
                    InsightTone.Warning -> "Review"
                    InsightTone.Neutral -> "Detail"
                },
                actionColor = tint
            )
        }

        if (insights.isEmpty()) {
            AlertItem(
                icon = Icons.Rounded.Info,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                title = "No insights yet",
                subtitle = "Track more activity to unlock personalized financial guidance.",
                trailingIcon = Icons.Rounded.ChevronRight
            )
        }
    }
}

@Composable
private fun AlertItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    actionText: String? = null,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                actionText != null -> {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelMedium,
                        color = actionColor
                    )
                }
                trailingIcon != null -> {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularProgressRing(
    progress: Float,
    strokeWidth: androidx.compose.ui.unit.Dp,
    trackColor: Color,
    progressColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
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
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = stroke
        )
    }
}

@Composable
private fun GradientProgressRing(progress: Float) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
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
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = stroke
        )
    }
}

@Composable
private fun GoalProgressBar(progress: Float) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
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
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Brush.horizontalGradient(colors = gradientColors))
        )
    }
}

internal fun formatLkr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.ENGLISH).apply {
        maximumFractionDigits = 0
    }
    return "LKR ${formatter.format(amount)}"
}

private fun openReport(context: android.content.Context) {
    context.startActivity(
        android.content.Intent(context, ReportDetailActivity::class.java)
    )
}
