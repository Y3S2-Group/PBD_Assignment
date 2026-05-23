package com.example.financeapp.ui.expense

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.financeapp.domain.model.Expense
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
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
    fun initialState_isEmpty() = testScope.runTest {
        val viewModel = ExpenseViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.expenses.isEmpty())
            assertEquals(0.0, state.committedTotal, 0.0)
            assertEquals(0.0, state.discretionaryTotal, 0.0)
        }
    }

    @Test
    fun addExpense_updatesState() = testScope.runTest {
        val viewModel = ExpenseViewModel()
        val expense = Expense(
            id = "exp_8",
            amountLkr = 1500.0,
            category = "Food",
            spendingType = "DISCRETIONARY",
            paymentMethod = "Card",
            timestamp = System.currentTimeMillis()
        )

        viewModel.state.test {
            awaitItem()
            viewModel.addExpense(expense)
            testScope.testScheduler.advanceUntilIdle()
            val updated = withTimeout(1_000) { awaitItem() }
            assertEquals(1, updated.expenses.size)
        }
    }

    @Test
    fun committedVsDiscretionaryTotals_calculated() = testScope.runTest {
        val viewModel = ExpenseViewModel()
        val committed = Expense(
            id = "exp_9",
            amountLkr = 2000.0,
            category = "Rent",
            spendingType = "COMMITTED",
            paymentMethod = "Transfer",
            timestamp = System.currentTimeMillis()
        )
        val discretionary = Expense(
            id = "exp_10",
            amountLkr = 500.0,
            category = "Coffee",
            spendingType = "DISCRETIONARY",
            paymentMethod = "Cash",
            timestamp = System.currentTimeMillis()
        )

        viewModel.state.test {
            awaitItem()
            viewModel.addExpense(committed)
            viewModel.addExpense(discretionary)
            testScope.testScheduler.advanceUntilIdle()
            val updated = withTimeout(1_000) { awaitItem() }
            assertEquals(2000.0, updated.committedTotal, 0.01)
            assertEquals(500.0, updated.discretionaryTotal, 0.01)
        }
    }
}

