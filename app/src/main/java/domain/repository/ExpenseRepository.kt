package domain.repository

import data.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun addExpense(expense: Expense): Result<String>
    fun getExpenses(userId: String): Flow<List<Expense>>
}
