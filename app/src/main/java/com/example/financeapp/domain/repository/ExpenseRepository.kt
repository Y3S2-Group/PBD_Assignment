package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.Expense

interface ExpenseRepository {
    suspend fun insertExpense(expense: Expense)
    suspend fun getAllExpenses(): List<Expense>
    suspend fun getBySpendingType(spendingType: String): List<Expense>
    suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double
}
