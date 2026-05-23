package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val amountLkr: Double,
    val category: String,
    val spendingType: String,
    val paymentMethod: String,
    val timestamp: Long
)

