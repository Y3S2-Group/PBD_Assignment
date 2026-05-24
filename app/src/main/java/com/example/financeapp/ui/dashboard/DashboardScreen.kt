package com.example.financeapp.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.ui.auth.AuthViewModel
import com.example.financeapp.ui.components.GlobalTopAppBar
import com.example.financeapp.ui.theme.AppTheme
import com.example.financeapp.ui.dashboard.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// ─── Category colour palette ──────────────────────────────────────────────────
private val CATEGORY_PALETTE = listOf(
    Color(0xFF6750A4), Color(0xFF4CAF50), Color(0xFFFF9800),
    Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF00BCD4),
    Color(0xFFFF5722), Color(0xFF9E9E9E)
)

@Composable
fun DashboardScreen(
    avatarId: String,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val dashVm: DashboardViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val state by dashVm.state.collectAsState()
    val displayName = currentUser?.displayName.orEmpty()
    val email = currentUser?.email.orEmpty()
    val firstName = displayName.substringBefore(" ").ifBlank {
        email.substringBefore("@").ifBlank { "User" }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp)
        ) {
            GlobalTopAppBar(
                title = "Vault",
                subtitle = "Hey $firstName,",
                healthScore = state.healthScore,
                avatarId = avatarId,
                onProfileClick = onProfileClick,
                onNotificationClick = onNotificationClick,
            )
            Spacer(modifier = Modifier.height(24.dp))

            // FR1 — at-a-glance balance cards
            BalanceCards(state)
            Spacer(modifier = Modifier.height(24.dp))

            // FR1 — goal progress (shown only when a goal exists)
            if (state.hasGoal) {
                GoalProgressWidget(state)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // FR2 & FR3 — donut + committed/discretionary split
            DataVizSection(state)
            Spacer(modifier = Modifier.height(24.dp))

            // FR4 — 6-month income vs expense trend line chart
            TrendChartCard(state.monthlyTrend)
            Spacer(modifier = Modifier.height(24.dp))

            // FR5 — top spending categories
            if (state.topCategories.isNotEmpty()) {
                TopCategoriesCard(state.topCategories, state.incomeThisMonth)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // FR6 — income source breakdown
            if (state.incomeBySource.isNotEmpty()) {
                IncomeSourceCard(state.incomeBySource)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Smart insights derived from real data
            SmartInsightsCard(state)
        }
    }
}


// ─── FR1 Balance cards ────────────────────────────────────────────────────────

@Composable
private fun BalanceCards(state: DashboardUiState) {
    val prevIncome = state.monthlyTrend.getOrNull(4)?.income ?: 0.0
    val incomeDelta = if (prevIncome > 0)
        ((state.incomeThisMonth - prevIncome) / prevIncome * 100).toInt() else 0
    val budgetUsedPct = if (state.incomeThisMonth > 0)
        (state.expenseThisMonth / state.incomeThisMonth * 100).toInt() else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Income This Month",
            amount = formatLkr(state.incomeThisMonth),
            subtitle = if (incomeDelta >= 0) "+$incomeDelta% from last month"
                       else "$incomeDelta% from last month",
            amountColor = MaterialTheme.colorScheme.secondary,
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            iconTint = MaterialTheme.colorScheme.secondary
        )
        MetricCard(
            title = "Expenses",
            amount = formatLkr(state.expenseThisMonth),
            subtitle = "$budgetUsedPct% of income spent",
            amountColor = MaterialTheme.colorScheme.onSurface,
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            iconTint = MaterialTheme.colorScheme.tertiary
        )
        MetricCard(
            title = "Net Savings",
            amount = formatLkr(state.netSavingsThisMonth),
            subtitle = if (state.hasGoal) "Goal: ${formatLkr(state.goalTargetAmount)}"
                       else "Keep it up!",
            amountColor = if (state.netSavingsThisMonth >= 0) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.error,
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
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(20.dp),
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
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

// ─── FR1 Goal progress widget ─────────────────────────────────────────────────

@Composable
private fun GoalProgressWidget(state: DashboardUiState) {
    val progress = (state.goalProgressPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900),
        label = "goalRing"
    )

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
                .offset(x = 60.dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
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
            Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
                GradientProgressRing(progress = animatedProgress)
                Text(
                    text = "${state.goalProgressPercent.toInt()}%",
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
                        text = if (state.goalProgressPercent >= 70) "Goal in Sight!" else "Keep Saving",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = state.goalName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatLkr(state.goalCurrentSavings)} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatLkr(state.goalTargetAmount)} target",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    GoalProgressBar(progress = animatedProgress)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                text = "${state.goalDaysRemaining} days left",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Add Funds",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ─── FR2 & FR3 Data viz section ───────────────────────────────────────────────

@Composable
private fun DataVizSection(state: DashboardUiState) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth > 700.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DonutBreakdownCard(state, modifier = Modifier.weight(1f))
                BudgetTypeCard(state, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                DonutBreakdownCard(state, modifier = Modifier.fillMaxWidth())
                BudgetTypeCard(state, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// FR2 — donut chart with real category spend
@Composable
private fun DonutBreakdownCard(state: DashboardUiState, modifier: Modifier) {
    val entries = state.categorySpend.entries.sortedByDescending { it.value }
    val total = entries.sumOf { it.value }.coerceAtLeast(1.0)

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "Monthly Breakdown", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(24.dp))
            if (entries.isEmpty()) {
                EmptyState("No expenses recorded this month")
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DonutChart(entries = entries, total = total)
                    Spacer(modifier = Modifier.height(20.dp))
                    CategoryLegend(entries = entries, total = total)
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    entries: List<Map.Entry<String, Double>>,
    total: Double
) {
    Box(
        modifier = Modifier
            .size(192.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 40f, cap = StrokeCap.Butt)
            val gap = Stroke(width = 40f, cap = StrokeCap.Butt)
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 24f
            val boxSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            var startAngle = -90f
            entries.forEachIndexed { idx, (_, amount) ->
                val sweep = (amount / total * 360f).toFloat()
                drawArc(
                    color = CATEGORY_PALETTE[idx % CATEGORY_PALETTE.size],
                    startAngle = startAngle + 1f,
                    sweepAngle = sweep - 2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = boxSize,
                    style = stroke
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatLkr(entries.sumOf { it.value }),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun CategoryLegend(
    entries: List<Map.Entry<String, Double>>,
    total: Double
) {
    val shown = entries.take(6)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                pair.forEachIndexed { pairIdx, (name, amount) ->
                    val globalIdx = shown.indexOf(pair.first()) + pairIdx
                    LegendItem(
                        label = "$name (${(amount / total * 100).toInt()}%)",
                        color = CATEGORY_PALETTE[globalIdx % CATEGORY_PALETTE.size]
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// FR3 — committed vs discretionary split
@Composable
private fun BudgetTypeCard(state: DashboardUiState, modifier: Modifier) {
    val totalSpend = (state.committedSpend + state.discretionarySpend).coerceAtLeast(1.0)
    val discPct = (state.discretionarySpend / totalSpend).toFloat().coerceIn(0f, 1f)
    val commPct = 1f - discPct
    val hasData = state.committedSpend > 0 || state.discretionarySpend > 0

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(text = "Budget Type", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "How your spending is allocated",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!hasData) {
                EmptyState("No expense data this month")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Discretionary (${(discPct * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Committed (${(commPct * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Stacked bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(CircleShape)
                    ) {
                        if (discPct > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(discPct)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (discPct > 0.15f) {
                                    Text(
                                        text = "WANT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        if (commPct > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(commPct)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (commPct > 0.15f) {
                                    Text(
                                        text = "NEED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    // Amount labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatLkr(state.discretionarySpend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatLkr(state.committedSpend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Insight tip
                val tip = buildCommittedInsight(state)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun buildCommittedInsight(state: DashboardUiState): String {
    val discPct = if ((state.committedSpend + state.discretionarySpend) > 0)
        state.discretionarySpend / (state.committedSpend + state.discretionarySpend) else 0.0
    return when {
        discPct > 0.6 -> "Over 60% of your spending is discretionary. Look for areas to cut back and redirect to savings."
        discPct < 0.3 -> "Most of your spending is committed (fixed). Good discipline on discretionary spend!"
        else -> "Your spending is balanced between needs and wants. Keep monitoring to maintain this ratio."
    }
}

// ─── FR4 — 6-month trend line chart ──────────────────────────────────────────

@Composable
private fun TrendChartCard(trend: List<MonthlyTrendPoint>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "6-Month Trend", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChartLegendDot("Income", Color(0xFF4CAF50))
                    ChartLegendDot("Expense", Color(0xFFE91E63))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (trend.all { it.income == 0.0 && it.expense == 0.0 }) {
                EmptyState("No data yet — add income and expenses to see the trend")
            } else {
                LineChart(trend = trend)
                Spacer(modifier = Modifier.height(12.dp))
                // Month labels row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trend.forEach { pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LineChart(trend: List<MonthlyTrendPoint>) {
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFE91E63)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val maxVal = trend.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0).toFloat()
        val n = trend.size
        val padTop = size.height * 0.08f
        val padBottom = size.height * 0.08f
        val chartH = size.height - padTop - padBottom
        val stepX = if (n > 1) size.width / (n - 1).toFloat() else size.width

        fun yFor(v: Double) = padTop + chartH - (v.toFloat() / maxVal * chartH)

        // Horizontal grid lines (4 lines)
        repeat(4) { i ->
            val y = padTop + chartH * i / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        // Build paths
        val incomePath = Path()
        val expensePath = Path()
        trend.forEachIndexed { i, pt ->
            val x = i * stepX
            val yI = yFor(pt.income)
            val yE = yFor(pt.expense)
            if (i == 0) {
                incomePath.moveTo(x, yI)
                expensePath.moveTo(x, yE)
            } else {
                // Cubic bezier for smooth curves
                val prevX = (i - 1) * stepX
                val cpX = (prevX + x) / 2f
                incomePath.cubicTo(cpX, yFor(trend[i - 1].income), cpX, yI, x, yI)
                expensePath.cubicTo(cpX, yFor(trend[i - 1].expense), cpX, yE, x, yE)
            }
        }

        val lineStroke = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(incomePath, incomeColor, style = lineStroke)
        drawPath(expensePath, expenseColor, style = lineStroke)

        // Dots and highlight for months where expense > income
        trend.forEachIndexed { i, pt ->
            val x = i * stepX
            val yI = yFor(pt.income)
            val yE = yFor(pt.expense)
            val exceeded = pt.expense > pt.income

            drawCircle(incomeColor, radius = 5f, center = Offset(x, yI))
            drawCircle(
                color = if (exceeded) expenseColor else expenseColor,
                radius = if (exceeded) 7f else 5f,
                center = Offset(x, yE)
            )
            if (exceeded) {
                drawCircle(expenseColor.copy(alpha = 0.25f), radius = 12f, center = Offset(x, yE))
            }
        }
    }
}

// ─── FR5 — Top spending categories ───────────────────────────────────────────

@Composable
private fun TopCategoriesCard(
    categories: List<CategorySpendItem>,
    incomeThisMonth: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Top Spending Categories", style = MaterialTheme.typography.titleLarge)
            categories.forEachIndexed { idx, item ->
                TopCategoryRow(
                    rank = idx + 1,
                    item = item,
                    incomeThisMonth = incomeThisMonth,
                    color = CATEGORY_PALETTE[idx % CATEGORY_PALETTE.size]
                )
            }
        }
    }
}

@Composable
private fun TopCategoryRow(
    rank: Int,
    item: CategorySpendItem,
    incomeThisMonth: Double,
    color: Color
) {
    val incomePercent = if (incomeThisMonth > 0) (item.amount / incomeThisMonth * 100).toInt() else 0
    val isSurprising = incomePercent >= 15 || item.percent >= 30.0

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSurprising && incomeThisMonth > 0) {
                        Text(
                            text = "${formatLkr(item.amount)} — $incomePercent% of your income",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (incomePercent >= 20) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatLkr(item.amount),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.percent.toInt()}% of spend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Progress bar showing share of total spend
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (item.percent / 100.0).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

// ─── FR6 — Income source breakdown ────────────────────────────────────────────

@Composable
private fun IncomeSourceCard(incomeBySource: Map<String, Double>) {
    val total = incomeBySource.values.sum().coerceAtLeast(1.0)
    val entries = incomeBySource.entries.sortedByDescending { it.value }
    val sourceColors = listOf(
        Color(0xFF6750A4), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF00BCD4)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Income Sources", style = MaterialTheme.typography.titleLarge)

            // Stacked bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(CircleShape)
            ) {
                entries.forEachIndexed { idx, (_, amount) ->
                    val weight = (amount / total).toFloat().coerceAtLeast(0.001f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(sourceColors[idx % sourceColors.size])
                    )
                }
            }

            // Legend rows
            entries.forEachIndexed { idx, (source, amount) ->
                val pct = (amount / total * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(sourceColors[idx % sourceColors.size])
                        )
                        Text(
                            text = source.replaceFirstChar { it.titlecase(Locale.US) },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatLkr(amount),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

// ─── Smart insights (FR7-adjacent) ────────────────────────────────────────────

@Composable
private fun SmartInsightsCard(state: DashboardUiState) {
    val insights = buildInsights(state)
    if (insights.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Alerts & Insights", style = MaterialTheme.typography.titleLarge)
        }
        insights.forEach { insight ->
            InsightRow(insight)
        }
    }
}

data class InsightItem(
    val title: String,
    val subtitle: String,
    val isWarning: Boolean
)

private fun buildInsights(state: DashboardUiState): List<InsightItem> {
    val list = mutableListOf<InsightItem>()
    if (state.isLoading) return list

    // Spending vs income
    if (state.expenseThisMonth > state.incomeThisMonth && state.incomeThisMonth > 0) {
        val overspend = state.expenseThisMonth - state.incomeThisMonth
        list += InsightItem(
            "Overspent This Month",
            "You spent ${formatLkr(overspend)} more than you earned.",
            isWarning = true
        )
    } else if (state.netSavingsThisMonth > 0) {
        list += InsightItem(
            "Positive Savings",
            "You saved ${formatLkr(state.netSavingsThisMonth)} this month. Great job!",
            isWarning = false
        )
    }

    // Top category insight
    val top = state.topCategories.firstOrNull()
    if (top != null && state.incomeThisMonth > 0) {
        val incomePct = (top.amount / state.incomeThisMonth * 100).toInt()
        if (incomePct >= 15) {
            list += InsightItem(
                "High ${top.name} Spend",
                "${top.name}: ${formatLkr(top.amount)} — $incomePct% of your income.",
                isWarning = incomePct >= 25
            )
        }
    }

    // Goal milestone
    if (state.hasGoal) {
        when {
            state.goalProgressPercent >= 100.0 -> list += InsightItem(
                "Goal Reached! 🎉",
                "You've fully funded your ${state.goalName} goal.",
                isWarning = false
            )
            state.goalProgressPercent >= 75.0 -> list += InsightItem(
                "Almost There",
                "${state.goalName} is ${state.goalProgressPercent.toInt()}% funded. Final push!",
                isWarning = false
            )
        }
    }

    // Health score
    if (state.healthScore < 40 && !state.isLoading) {
        list += InsightItem(
            "Low Financial Health Score",
            "Your score is ${state.healthScore}/100. Try saving more or reducing discretionary spend.",
            isWarning = true
        )
    }

    return list.take(4)
}

@Composable
private fun InsightRow(insight: InsightItem) {
    val iconTint = if (insight.isWarning) MaterialTheme.colorScheme.error
                   else MaterialTheme.colorScheme.secondary
    val iconBg = if (insight.isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                 else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    val icon = if (insight.isWarning) Icons.Rounded.Warning else Icons.Rounded.CheckCircle

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = insight.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Shared drawing primitives ────────────────────────────────────────────────

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
        val sz = Size(diameter, diameter)
        drawArc(trackColor, -90f, 360f, false, topLeft, sz, style = stroke)
        drawArc(progressColor, -90f, 360f * progress, false, topLeft, sz, style = stroke)
    }
}

@Composable
private fun GradientProgressRing(progress: Float) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = Stroke(width = 8f, cap = StrokeCap.Round)
        val diameter = size.minDimension
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val sz = Size(diameter, diameter)
        drawArc(trackColor, -90f, 360f, false, topLeft, sz, style = stroke)
        drawArc(Brush.sweepGradient(gradientColors), -90f, 360f * progress, false, topLeft, sz, style = stroke)
    }
}

@Composable
private fun GoalProgressBar(progress: Float) {
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
                .fillMaxWidth(progress)
                .background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                )
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Formatting helpers ───────────────────────────────────────────────────────

private fun formatLkr(amount: Double): String {
    val abs = abs(amount)
    val prefix = if (amount < 0) "-" else ""
    return when {
        abs >= 1_000_000 -> "${prefix}LKR ${String.format(Locale.US, "%.1f", abs / 1_000_000)}M"
        abs >= 1_000 -> "${prefix}LKR ${String.format(Locale.US, "%.1f", abs / 1_000)}K"
        else -> "${prefix}LKR ${NumberFormat.getNumberInstance(Locale.US).format(abs)}"
    }
}
