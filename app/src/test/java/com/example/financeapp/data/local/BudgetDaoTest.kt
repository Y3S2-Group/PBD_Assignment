package com.example.financeapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.financeapp.domain.model.Budget
import com.example.financeapp.domain.model.Goal
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, IncomeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        goalDao = database.goalDao()
        budgetDao = database.budgetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveGoal() = kotlinx.coroutines.runBlocking {
        val goal = Goal(
            id = "goal_1",
            name = "MacBook Pro M4",
            targetAmount = 490000.0,
            currentSavings = 0.0,
            deadlineTimestamp = 1_750_000_000_000
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
            targetAmount = 490000.0,
            currentSavings = 10000.0,
            deadlineTimestamp = 1_750_000_000_000
        )

        goalDao.insert(goal)
        goalDao.updateCurrentSavings("goal_2", 45000.0)
        val updated = goalDao.getById("goal_2")

        assertNotNull(updated)
        assertEquals(45000.0, updated?.currentSavings ?: 0.0, 0.01)
    }

    @Test
    fun insertAndRetrieveBudget() = kotlinx.coroutines.runBlocking {
        val budget = Budget(
            id = "budget_1",
            monthYear = "2026-05",
            categoryAllocationsJson = "{\"Food\":12000,\"Transport\":8000}",
            expectedIncome = 250000.0
        )

        budgetDao.insert(budget)
        val retrieved = budgetDao.getByMonthYear("2026-05")

        assertNotNull(retrieved)
        assertEquals(budget, retrieved)
    }
}

