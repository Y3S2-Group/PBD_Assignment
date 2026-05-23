package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_categories")
data class BudgetCategory(
    @PrimaryKey val id: String,
    val categoryName: String,
    val allocatedAmount: Double,
    val monthYear: String
)

