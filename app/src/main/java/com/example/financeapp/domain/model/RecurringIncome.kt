package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Template for an income that repeats on a fixed day of the month. */
@Entity(tableName = "recurring_incomes")
data class RecurringIncome(
    @PrimaryKey val id: String,
    val sourceType: String,
    val sourceLabel: String? = null,
    val currency: String,
    val defaultAmount: Double,
    /** Day of month this income typically arrives (1–28). */
    val dayOfMonth: Int,
    val isActive: Boolean = true,
    val createdAt: Long,
)
