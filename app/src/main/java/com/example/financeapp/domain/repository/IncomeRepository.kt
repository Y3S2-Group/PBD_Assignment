package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.Income

interface IncomeRepository {
    suspend fun insertIncome(income: Income)
    suspend fun updateIncome(income: Income)
    suspend fun deleteIncome(id: String)
    suspend fun getAllIncomes(): List<Income>
    suspend fun getBySourceType(sourceType: String): List<Income>
    suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double
}
