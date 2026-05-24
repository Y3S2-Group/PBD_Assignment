package com.example.financeapp.data.sync

import com.example.financeapp.data.local.BudgetDao
import com.example.financeapp.data.local.ExpenseDao
import com.example.financeapp.data.local.GoalDao
import com.example.financeapp.data.local.IncomeDao
import com.example.financeapp.data.local.RecurringIncomeDao
import com.example.financeapp.data.local.SavingsDepositDao
import com.example.financeapp.data.remote.FirestoreExpenseRepository
import com.example.financeapp.data.remote.FirestoreGoalRepository
import com.example.financeapp.data.remote.FirestoreIncomeRepository
import com.example.financeapp.util.AppEventBus
import com.example.financeapp.util.DataChangeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulls all user data from Firestore into the local Room cache.
 *
 * Called once after sign-in so the local database reflects any changes
 * made on other devices or sessions. Writes are intentionally fire-and-forget
 * so a Firestore outage never blocks the UI.
 */
@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestoreIncomeRepo: FirestoreIncomeRepository,
    private val firestoreExpenseRepo: FirestoreExpenseRepository,
    private val firestoreGoalRepo: FirestoreGoalRepository,
    private val incomeDao: IncomeDao,
    private val recurringIncomeDao: RecurringIncomeDao,
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao,
    private val budgetDao: BudgetDao,
    private val savingsDepositDao: SavingsDepositDao,
    private val eventBus: AppEventBus,
) {
    /**
     * Fetches all Firestore subcollections for [uid] and upserts them into Room.
     * Swallows network errors gracefully — the app continues with whatever
     * data is already in Room.
     */
    suspend fun syncAll(uid: String) = withContext(Dispatchers.IO) {
        try {
            val incomes = firestoreIncomeRepo.getAllIncomes(uid)
            incomes.forEach { incomeDao.insert(it) }

            val recurring = firestoreIncomeRepo.getAllRecurringIncomes(uid)
            recurring.forEach { recurringIncomeDao.insert(it) }

            val expenses = firestoreExpenseRepo.getAllExpenses(uid)
            expenses.forEach { expenseDao.insert(it) }

            val goals = firestoreGoalRepo.getAllGoals(uid)
            goals.forEach { goalDao.insert(it) }

            val categories = firestoreGoalRepo.getAllBudgetCategories(uid)
            categories.forEach { budgetDao.insert(it) }

            val deposits = firestoreGoalRepo.getAllDeposits(uid)
            deposits.forEach { savingsDepositDao.insert(it) }

            eventBus.send(DataChangeEvent.INCOME)
            eventBus.send(DataChangeEvent.EXPENSE)
            eventBus.send(DataChangeEvent.BUDGET_GOAL)
        } catch (e: Exception) {
            // Network unavailable — Room data is used as-is until next sync
        }
    }
}
