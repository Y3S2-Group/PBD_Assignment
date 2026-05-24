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
    val sourceLabel: String? = null,
    val date: Long,
    // ── Enhanced fields ───────────────────────────────────────────────────────
    val notes: String? = null,
    /** Freelance project name or client reference. */
    val projectRef: String? = null,
    /** Exchange rate (to LKR) used at time of entry. */
    val exchangeRate: Double = 1.0,
    /** True when this entry was created from a recurring income template. */
    val isRecurring: Boolean = false,
    /** For freelance entries: whether the invoice has been paid. */
    val invoicePaid: Boolean = true,
)
