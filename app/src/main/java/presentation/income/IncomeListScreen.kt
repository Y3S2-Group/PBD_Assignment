package presentation.income

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import presentation.income.components.IncomeListItem
import presentation.income.components.IncomeSummaryCard

@Composable
fun IncomeListScreen(
    state: IncomeUiState.Success,
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMonthlySummary()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            IncomeSummaryCard(
                summary = summary,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(state.incomes) { income ->
            IncomeListItem(income = income)
        }
    }
}
