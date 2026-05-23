package com.example.financeapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.SavingsDeposit
import java.time.YearMonth
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BudgetDaoTest {

    private lateinit var database: IncomeDatabase
    private lateinit var goalDao: GoalDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var depositDao: SavingsDepositDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, IncomeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        goalDao = database.goalDao()
        budgetDao = database.budgetDao()
        depositDao = database.savingsDepositDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // -------------------------------------------------------------------------
    // Goal DAO
    // -------------------------------------------------------------------------

    @Test
    fun insertAndRetrieveGoal() = kotlinx.coroutines.runBlocking {
        val goal = Goal(
            id = "goal_1",
            name = "MacBook Pro M4",
            targetAmount = 490_000.0,
            currentSavings = 0.0,
            currency = "LKR",
            deadlineTimestamp = 1_750_000_000_000,
            createdAt = 1_700_000_000_000
        )

        goalDao.insert(goal)
        val retrieved = goalDao.getById("goal_1")

        assertNotNull(retrieved)
        assertEquals(goal, retrieved)
    }

    @Test
    fun updateGoalSavings() = kotlinx.coroutines.runBlocking {
        val goal = Goal(
            id = "goal_2",
            name = "MacBook Pro M4",
            targetAmount = 490_000.0,
            currentSavings = 10_000.0,
            currency = "LKR",
            deadlineTimestamp = 1_750_000_000_000,
            createdAt = 1_700_000_000_000
        )

        goalDao.insert(goal)
        goalDao.updateCurrentSavings("goal_2", 45_000.0)
        val updated = goalDao.getById("goal_2")

        assertNotNull(updated)
        assertEquals(45_000.0, updated?.currentSavings ?: 0.0, 0.01)
    }

    @Test
    fun getLatestGoal_returnsNewest() = kotlinx.coroutines.runBlocking {
        goalDao.insert(Goal("g_old", "Old", 100_000.0, 0.0, "LKR", 2_000_000_000_000, 1_600_000_000_000))
        goalDao.insert(Goal("g_new", "New", 200_000.0, 0.0, "LKR", 2_000_000_000_000, 1_700_000_000_000))

        val latest = goalDao.getLatest()
        assertEquals("g_new", latest?.id)
    }

    @Test
    fun getById_returnsNull_forMissingGoal() = kotlinx.coroutines.runBlocking {
        assertNull(goalDao.getById("nonexistent"))
    }

    @Test
    fun goal_storesCurrencyField() = kotlinx.coroutines.runBlocking {
        val goal = Goal("g_usd", "iPhone", 300_000.0, 0.0, "USD", 1_800_000_000_000, 1_700_000_000_000)
        goalDao.insert(goal)
        val retrieved = goalDao.getById("g_usd")
        assertEquals("USD", retrieved?.currency)
    }

    // -------------------------------------------------------------------------
    // Budget Category DAO
    // -------------------------------------------------------------------------

    @Test
    fun insertAndRetrieveBudgetCategory() = kotlinx.coroutines.runBlocking {
        val budget = BudgetCategory(
            id = "budget_food_2026_05",
            categoryName = "Food",
            allocatedAmount = 12_000.0,
            monthYear = "2026-05"
        )

        budgetDao.insert(budget)
        val retrieved = budgetDao.getByMonthYear("2026-05")

        assertEquals(1, retrieved.size)
        assertEquals(budget, retrieved.first())
    }

    @Test
    fun insertBudgetCategory_replacesOnConflict() = kotlinx.coroutines.runBlocking {
        val original = BudgetCategory("id_1", "Food", 10_000.0, "2026-05")
        val updated = BudgetCategory("id_1", "Food", 15_000.0, "2026-05")

        budgetDao.insert(original)
        budgetDao.insert(updated)

        val results = budgetDao.getByMonthYear("2026-05")
        assertEquals(1, results.size)
        assertEquals(15_000.0, results.first().allocatedAmount, 0.01)
    }

    @Test
    fun getBudgetCategories_filtersCorrectMonth() = kotlinx.coroutines.runBlocking {
        budgetDao.insert(BudgetCategory("id_may", "Food", 10_000.0, "2026-05"))
        budgetDao.insert(BudgetCategory("id_jun", "Food", 12_000.0, "2026-06"))

        val mayResults = budgetDao.getByMonthYear("2026-05")
        val junResults = budgetDao.getByMonthYear("2026-06")

        assertEquals(1, mayResults.size)
        assertEquals(1, junResults.size)
        assertEquals("2026-05", mayResults.first().monthYear)
    }

    // -------------------------------------------------------------------------
    // Savings Deposit DAO
    // -------------------------------------------------------------------------

    @Test
    fun insertAndRetrieveDeposit() = kotlinx.coroutines.runBlocking {
        val deposit = SavingsDeposit(
            id = "dep_1",
            goalId = "goal_1",
            amount = 25_000.0,
            timestamp = 1_700_000_000_000
        )

        depositDao.insert(deposit)
        val results = depositDao.getAllForGoal("goal_1")

        assertEquals(1, results.size)
        assertEquals(deposit, results.first())
    }

    @Test
    fun getAllForGoal_filtersCorrectGoal() = kotlinx.coroutines.runBlocking {
        depositDao.insert(SavingsDeposit("d1", "goal_A", 10_000.0, 1_700_000_001_000))
        depositDao.insert(SavingsDeposit("d2", "goal_B", 20_000.0, 1_700_000_002_000))
        depositDao.insert(SavingsDeposit("d3", "goal_A", 15_000.0, 1_700_000_003_000))

        val goalADeposits = depositDao.getAllForGoal("goal_A")
        assertEquals(2, goalADeposits.size)

        val goalBDeposits = depositDao.getAllForGoal("goal_B")
        assertEquals(1, goalBDeposits.size)
    }

    @Test
    fun sumAmountBetween_sumsDepositsInRange() = kotlinx.coroutines.runBlocking {
        val zone = ZoneId.systemDefault()
        val thisMonth = YearMonth.now()
        val start = thisMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val mid = thisMonth.atDay(15).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = thisMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val beforeRange = start - 1_000

        depositDao.insert(SavingsDeposit("in_1", "g1", 10_000.0, mid))
        depositDao.insert(SavingsDeposit("in_2", "g1", 5_000.0, start))
        depositDao.insert(SavingsDeposit("out_1", "g1", 50_000.0, beforeRange))

        val total = depositDao.sumAmountBetween("g1", start, end)
        assertEquals(15_000.0, total ?: 0.0, 0.01)
    }

    @Test
    fun sumAmountBetween_returnsNull_whenNoDepositsInRange() = kotlinx.coroutines.runBlocking {
        val result = depositDao.sumAmountBetween("no_goal", 0L, 1_000L)
        assertNull(result)
    }

    @Test
    fun insertDeposit_replacesOnConflict() = kotlinx.coroutines.runBlocking {
        val original = SavingsDeposit("d1", "g1", 10_000.0, 1_700_000_000_000)
        val updated = SavingsDeposit("d1", "g1", 20_000.0, 1_700_000_000_000)

        depositDao.insert(original)
        depositDao.insert(updated)

        val results = depositDao.getAllForGoal("g1")
        assertEquals(1, results.size)
        assertEquals(20_000.0, results.first().amount, 0.01)
    }
}
