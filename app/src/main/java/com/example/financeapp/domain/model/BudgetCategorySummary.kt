package com.example.financeapp.domain.model

data class BudgetCategorySummary(
    val categoryName: String,
    val allocatedAmount: Double,
    val actualSpent: Double
)

