package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.model.RecurringIncome

interface IncomeRepository {
    // ── Income CRUD ───────────────────────────────────────────────────────────
    suspend fun insertIncome(income: Income)
    suspend fun updateIncome(income: Income)
    suspend fun deleteIncome(id: String)
    suspend fun getAllIncomes(): List<Income>
    suspend fun getBySourceType(sourceType: String): List<Income>
    suspend fun getBySourceTypes(sourceTypes: List<String>): List<Income>
    suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double

    // ── Recurring income ──────────────────────────────────────────────────────
    suspend fun insertRecurringIncome(recurring: RecurringIncome)
    suspend fun getActiveRecurringIncomes(): List<RecurringIncome>
    suspend fun getAllRecurringIncomes(): List<RecurringIncome>
    suspend fun deactivateRecurringIncome(id: String)
    suspend fun deleteRecurringIncome(id: String)
}
