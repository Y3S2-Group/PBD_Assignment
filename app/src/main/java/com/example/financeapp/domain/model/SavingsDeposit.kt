package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_deposits")
data class SavingsDeposit(
    @PrimaryKey val id: String,
    val goalId: String,
    val amount: Double,
    val timestamp: Long
)
