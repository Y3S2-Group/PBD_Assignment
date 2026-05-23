package com.example.financeapp.data.repository

import com.example.financeapp.data.local.ExpenseDao
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.repository.ExpenseRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override suspend fun insertExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            expenseDao.insert(expense)
        }
    }

    override suspend fun getAllExpenses(): List<Expense> = withContext(Dispatchers.IO) {
        expenseDao.getAll()
    }

    override suspend fun getBySpendingType(spendingType: String): List<Expense> = withContext(Dispatchers.IO) {
        expenseDao.getBySpendingType(spendingType)
    }

    override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
        withContext(Dispatchers.IO) {
            expenseDao.sumAmountLkrBetween(startInclusive, endInclusive) ?: 0.0
        }
}

