package com.example.financeapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                ReportDetailRoute(
                    onBack = ::finish
                )
            }
        }
    }
}

@Composable
private fun ReportDetailRoute(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    when (val currentState = state) {
        DashboardUiState.Loading -> DashboardLoadingState()
        is DashboardUiState.Error -> DashboardErrorState(
            message = currentState.message,
            onRetry = viewModel::loadDashboard
        )
        is DashboardUiState.Success -> ReportDetailScreen(
            summary = currentState.summary,
            onBack = onBack,
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, buildShareSummary(currentState.summary))
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share summary"))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDetailScreen(
    summary: com.example.financeapp.domain.model.DashboardAnalytics,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share report"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummarySection(summary)
            }
            item {
                BreakdownSection(
                    title = "Top Expense Categories",
                    items = summary.categoryBreakdown.map {
                        "${it.label}: ${formatLkr(it.amountLkr)} (${it.percentage.toInt()}%)"
                    }
                )
            }
            item {
                BreakdownSection(
                    title = "Income Sources",
                    items = summary.incomeBreakdown.map {
                        "${it.label}: ${formatLkr(it.amountLkr)} (${it.percentage.toInt()}%)"
                    }
                )
            }
            item {
                BreakdownSection(
                    title = "Trend Overview",
                    items = summary.monthlyTrend.map {
                        "${it.label}: income ${formatLkr(it.incomeLkr)}, expenses ${formatLkr(it.expenseLkr)}, savings ${formatLkr(it.savingsLkr)}"
                    }
                )
            }
            item {
                BreakdownSection(
                    title = "Insights",
                    items = summary.insights.map { "${it.title}: ${it.message}" }
                )
            }
        }
    }
}

@Composable
private fun SummarySection(summary: com.example.financeapp.domain.model.DashboardAnalytics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Financial Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text("Income: ${formatLkr(summary.totalIncomeLkr)}")
            Text("Expenses: ${formatLkr(summary.totalExpenseLkr)}")
            Text("Net savings: ${formatLkr(summary.netSavingsLkr)}")
            Text("Savings rate: ${summary.savingsRate}%")
            Text("Health score: ${summary.healthScore}")
            Text("Goal: ${summary.goal.title} (${(summary.goal.progress * 100).toInt()}%)")
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Committed: ${summary.spendingBreakdown.committedPercentage.toInt()}%")
                Text("Discretionary: ${summary.spendingBreakdown.discretionaryPercentage.toInt()}%")
            }
        }
    }
}

@Composable
private fun BreakdownSection(
    title: String,
    items: List<String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (items.isEmpty()) {
                Text(
                    text = "No data available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    Text(text = item)
                }
            }
        }
    }
}

private fun buildShareSummary(summary: com.example.financeapp.domain.model.DashboardAnalytics): String {
    return buildString {
        appendLine("Monthly Financial Summary")
        appendLine("Income: ${formatLkr(summary.totalIncomeLkr)}")
        appendLine("Expenses: ${formatLkr(summary.totalExpenseLkr)}")
        appendLine("Net savings: ${formatLkr(summary.netSavingsLkr)}")
        appendLine("Savings rate: ${summary.savingsRate}%")
        appendLine("Health score: ${summary.healthScore}")
        summary.insights.forEach { insight ->
            appendLine("- ${insight.title}: ${insight.message}")
        }
    }
}
