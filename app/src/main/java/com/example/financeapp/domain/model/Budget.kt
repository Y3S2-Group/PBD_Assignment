package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val id: String,
    val monthYear: String,
    val categoryAllocationsJson: String,
    val expectedIncome: Double
)

