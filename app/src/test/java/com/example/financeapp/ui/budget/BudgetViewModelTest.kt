package com.example.financeapp.ui.budget

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

    private class FakeBudgetRepository(
        seedGoal: Goal
    ) : BudgetRepository {
        private var goal: Goal? = seedGoal
        private val categories = mutableListOf<BudgetCategory>()

        override suspend fun upsertGoal(goal: Goal) {
            this.goal = goal
        }

        override suspend fun getLatestGoal(): Goal? = goal

        override suspend fun updateGoalSavings(id: String, newSavings: Double) {
            goal = goal?.copy(currentSavings = newSavings)
        }

        override suspend fun upsertBudgetCategory(category: BudgetCategory) {
            categories.removeAll { it.id == category.id }
            categories.add(category)
        }

        override suspend fun getBudgetCategoriesForMonth(monthYear: String): List<BudgetCategory> {
            return categories.filter { it.monthYear == monthYear }
        }

        override suspend fun getActualSpentByCategory(monthYear: String): Map<String, Double> {
            return emptyMap()
        }
    }

    @Test
    fun requiredMonthlySavings_isCalculated() = testScope.runTest {
        val goal = Goal(
            id = "goal_seed",
            name = "MacBook Pro M4",
            targetAmount = 120000.0,
            currentSavings = 0.0,
            deadlineTimestamp = 1_750_000_000_000,
            createdAt = 1_700_000_000_000
        )
        val viewModel = BudgetViewModel(FakeBudgetRepository(goal))

        val required = viewModel.calculateRequiredMonthlySavings(goal, monthsRemaining = 12)
        assertEquals(10000.0, required, 0.01)
    }

    @Test
    fun addSavings_updatesGoalState() = testScope.runTest {
        val goal = Goal(
            id = "goal_5",
            name = "MacBook Pro M4",
            targetAmount = 490000.0,
            currentSavings = 0.0,
            deadlineTimestamp = 1_750_000_000_000,
            createdAt = 1_700_000_000_000
        )
        val viewModel = BudgetViewModel(FakeBudgetRepository(goal))

        testScope.testScheduler.advanceUntilIdle()
        viewModel.addSavingsToGoal(amount = 25000.0)
        testScope.testScheduler.advanceUntilIdle()

        val updated = viewModel.state.value
        assertEquals(25000.0, updated.activeGoal?.currentSavings ?: 0.0, 0.01)
    }
}
