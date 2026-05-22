package domain.repository

import data.model.Expense
import data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun addExpense(expense: Expense): Result<String>
    fun getExpenses(userId: String): Flow<List<Expense>>
    suspend fun getTotalExpenses(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Double
    suspend fun getExpensesByCategory(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Map<ExpenseCategory, Double>
    suspend fun getExpenses(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<Expense>
}
