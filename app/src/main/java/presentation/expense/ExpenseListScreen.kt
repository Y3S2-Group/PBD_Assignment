package presentation.expense

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import presentation.expense.components.ExpenseListItem
import data.model.Expense

// Placeholder for ExpenseUiState
@Composable
fun ExpenseListScreen(state: ExpenseUiState.Success) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.expenses) { expense ->
            ExpenseListItem(expense = expense)
        }
    }
}
