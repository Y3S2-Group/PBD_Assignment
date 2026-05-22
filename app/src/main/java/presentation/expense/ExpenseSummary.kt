package presentation.expense

import data.model.ExpenseCategory

data class ExpenseSummary(
    val total: Double = 0.0,
    val byCategory: Map<ExpenseCategory, Double> = emptyMap(),
    val discretionaryTotal: Double = 0.0,
    val committedTotal: Double = 0.0
)
