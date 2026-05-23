package com.example.financeapp.ui.budget

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.financeapp.domain.model.Goal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun requiredMonthlySavings_isCalculated() = testScope.runTest {
        val viewModel = BudgetViewModel()
        val goal = Goal(
            id = "goal_3",
            name = "MacBook Pro M4",
            targetAmount = 120000.0,
            currentSavings = 0.0,
            deadlineTimestamp = 1_750_000_000_000
        )

        val required = viewModel.calculateRequiredMonthlySavings(goal, monthsRemaining = 12)
        assertEquals(10000.0, required, 0.01)
    }

    @Test
    fun progressPercentage_isCalculated() = testScope.runTest {
        val viewModel = BudgetViewModel()
        val goal = Goal(
            id = "goal_4",
            name = "MacBook Pro M4",
            targetAmount = 490000.0,
            currentSavings = 98000.0,
            deadlineTimestamp = 1_750_000_000_000
        )

        val progress = viewModel.calculateProgressPercent(goal)
        assertEquals(20.0, progress, 0.01)
    }

    @Test
    fun addFunds_updatesGoalState() = testScope.runTest {
        val viewModel = BudgetViewModel()
        val goal = Goal(
            id = "goal_5",
            name = "MacBook Pro M4",
            targetAmount = 490000.0,
            currentSavings = 0.0,
            deadlineTimestamp = 1_750_000_000_000
        )

        viewModel.state.test {
            awaitItem()
            viewModel.addFunds(goalId = "goal_5", amount = 25000.0)
            testScope.testScheduler.advanceUntilIdle()
            val updated = withTimeout(1_000) { awaitItem() }
            assertEquals(25000.0, updated.goals.firstOrNull()?.currentSavings ?: 0.0, 0.01)
        }
    }
}

