package com.example.financeapp.data.sync

import com.example.financeapp.data.local.BudgetDao
import com.example.financeapp.data.local.ExpenseDao
import com.example.financeapp.data.local.GoalDao
import com.example.financeapp.data.local.IncomeDao
import com.example.financeapp.data.local.IncomeDatabase
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
 * Manages the boundary between Firestore (primary cloud store) and Room (local cache).
 *
 * WHY THIS CLASS EXISTS:
 * Room is a device-level SQLite database with no awareness of which Firebase user
 * is currently signed in. Without explicit clearing, data from a previous user session
 * (or a different account on the same device) leaks into the next session, causing
 * all users on the device to see merged data.
 *
 * ISOLATION STRATEGY:
 * 1. [clearAllUserData] — wipes every Room table. Called on sign-out and at the
 *    start of every sync so we never accumulate stale data from previous sessions.
 * 2. [syncAll] — clears Room first, then pulls the signed-in user's Firestore
 *    subcollections and repopulates Room exclusively with that user's data.
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
    private val database: IncomeDatabase,
    private val eventBus: AppEventBus,
) {
    /**
     * Wipes every table in the local Room database.
     *
     * Called in two situations:
     * - Sign-out: removes the current user's data so the next session (or next
     *   user on the same device) starts from a blank slate.
     * - Start of [syncAll]: ensures we never merge the current user's Firestore
     *   data on top of a different user's leftover rows.
     */
    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }

    /**
     * Clears the local cache, then fetches every Firestore subcollection for [uid]
     * and repopulates Room with only that user's data.
     *
     * After a successful sync, AppEventBus events are emitted so every ViewModel
     * refreshes its UI state. Network errors are swallowed — the UI stays on the
     * loading/empty state and the user can retry by pulling to refresh.
     */
    suspend fun syncAll(uid: String) = withContext(Dispatchers.IO) {
        // ── Step 1: wipe local cache before populating it with this user's data ──
        // This is the key isolation step. Without it, rows from a previous user
        // (or a previous session with stale/deleted data) would persist alongside
        // the freshly-fetched rows, causing data to "bleed" between accounts.
        database.clearAllTables()

        // ── Step 2: pull each Firestore subcollection and upsert into Room ────────
        // OnConflictStrategy.REPLACE on every DAO makes these calls idempotent.
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

            // ── Step 3: signal all ViewModels to re-read from the now-populated Room ─
            eventBus.send(DataChangeEvent.INCOME)
            eventBus.send(DataChangeEvent.EXPENSE)
            eventBus.send(DataChangeEvent.BUDGET_GOAL)
        } catch (e: Exception) {
            // Network unavailable — Room was cleared in Step 1.
            // ViewModels will show empty/loading state until the next sync attempt.
            // No data from any other user will be shown.
        }
    }
}
