package com.example.financeapp.data.repository

import com.example.financeapp.data.local.ExpenseDao
import com.example.financeapp.data.remote.FirestoreExpenseRepository
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.repository.ExpenseRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestoreRepo: FirestoreExpenseRepository,
) : ExpenseRepository {

    private val uid: String? get() = firebaseAuth.currentUser?.uid

    override suspend fun insertExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            expenseDao.insert(expense)
            uid?.let { id ->
                try { firestoreRepo.saveExpense(id, expense) } catch (e: Exception) { /* offline */ }
            }
        }
    }

    override suspend fun getAllExpenses(): List<Expense> = withContext(Dispatchers.IO) {
        expenseDao.getAll()
    }

    override suspend fun getBySpendingType(spendingType: String): List<Expense> =
        withContext(Dispatchers.IO) {
            expenseDao.getBySpendingType(spendingType)
        }

    override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
        withContext(Dispatchers.IO) {
            expenseDao.sumAmountLkrBetween(startInclusive, endInclusive) ?: 0.0
        }

    override suspend fun sumAmountLkrByCategoryBetween(start: Long, end: Long): Map<String, Double> =
        withContext(Dispatchers.IO) {
            expenseDao.sumAmountLkrByCategoryBetween(start, end)
                .associate { it.category to (it.total ?: 0.0) }
        }

    override suspend fun sumAmountLkrBySpendingTypeBetween(
        spendingType: String,
        start: Long,
        end: Long
    ): Double = withContext(Dispatchers.IO) {
        expenseDao.sumAmountLkrBySpendingTypeBetween(spendingType, start, end) ?: 0.0
    }
}
