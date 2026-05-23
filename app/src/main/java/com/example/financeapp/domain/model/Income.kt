package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey val id: String,
    val amount: Double,
    val currency: String,
    val amountLKR: Double,
    val sourceType: String,
    val date: Long
)

