package com.example.financeapp.data.repository

import com.example.financeapp.data.local.IncomeDao
import com.example.financeapp.data.local.RecurringIncomeDao
import com.example.financeapp.data.remote.FirestoreIncomeRepository
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.model.RecurringIncome
import com.example.financeapp.domain.repository.IncomeRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IncomeRepositoryImpl @Inject constructor(
    private val incomeDao: IncomeDao,
    private val recurringIncomeDao: RecurringIncomeDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestoreRepo: FirestoreIncomeRepository,
) : IncomeRepository {

    private val uid: String? get() = firebaseAuth.currentUser?.uid

    // ── Income CRUD ───────────────────────────────────────────────────────────

    override suspend fun insertIncome(income: Income) {
        withContext(Dispatchers.IO) {
            incomeDao.insert(income)
            uid?.let { id ->
                try { firestoreRepo.saveIncome(id, income) } catch (e: Exception) { /* offline */ }
            }
        }
    }

    override suspend fun updateIncome(income: Income) {
        withContext(Dispatchers.IO) {
            incomeDao.update(income)
            uid?.let { id ->
                try { firestoreRepo.saveIncome(id, income) } catch (e: Exception) { /* offline */ }
            }
        }
    }

    override suspend fun deleteIncome(id: String) {
        withContext(Dispatchers.IO) {
            incomeDao.deleteById(id)
            uid?.let { userId ->
                try { firestoreRepo.deleteIncome(userId, id) } catch (e: Exception) { /* offline */ }
            }
        }
    }

    override suspend fun getAllIncomes(): List<Income> =
        withContext(Dispatchers.IO) { incomeDao.getAll() }

    override suspend fun getBySourceType(sourceType: String): List<Income> =
        withContext(Dispatchers.IO) { incomeDao.getBySourceType(sourceType) }

    override suspend fun getBySourceTypes(sourceTypes: List<String>): List<Income> =
        withContext(Dispatchers.IO) { incomeDao.getBySourceTypes(sourceTypes) }

    override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
        withContext(Dispatchers.IO) {
            incomeDao.sumAmountLkrBetween(startInclusive, endInclusive) ?: 0.0
        }

    // ── Recurring income ──────────────────────────────────────────────────────

    override suspend fun insertRecurringIncome(recurring: RecurringIncome) {
        withContext(Dispatchers.IO) {
            recurringIncomeDao.insert(recurring)
            uid?.let { id ->
                try { firestoreRepo.saveRecurringIncome(id, recurring) } catch (e: Exception) { /* offline */ }
            }
        }
    }

    override suspend fun getActiveRecurringIncomes(): List<RecurringIncome> =
        withContext(Dispatchers.IO) { recurringIncomeDao.getAllActive() }

    override suspend fun getAllRecurringIncomes(): List<RecurringIncome> =
        withContext(Dispatchers.IO) { recurringIncomeDao.getAll() }

    override suspend fun deactivateRecurringIncome(id: String) {
        withContext(Dispatchers.IO) {
            recurringIncomeDao.deactivate(id)
            uid?.let { userId ->
                try { firestoreRepo.deactivateRecurringIncome(userId, id) } catch (e: Exception) { /* offline */ }
            }
        }
    }

    override suspend fun deleteRecurringIncome(id: String) {
        withContext(Dispatchers.IO) {
            recurringIncomeDao.deleteById(id)
            uid?.let { userId ->
                try { firestoreRepo.deleteRecurringIncome(userId, id) } catch (e: Exception) { /* offline */ }
            }
        }
    }
}
