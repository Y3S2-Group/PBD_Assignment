package com.example.financeapp.ui.expense

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.repository.ExpenseRepository
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

    private class FakeExpenseRepository(
        seed: List<Expense>
    ) : ExpenseRepository {
        private val entries = seed.toMutableList()

        override suspend fun insertExpense(expense: Expense) {
            entries.add(expense)
        }

        override suspend fun getAllExpenses(): List<Expense> = entries.toList()

        override suspend fun getBySpendingType(spendingType: String): List<Expense> =
            entries.filter { it.spendingType == spendingType }

        override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
            entries.filter { it.timestamp in startInclusive..endInclusive }.sumOf { it.amountLkr }
    }

    private fun seededRepository(): ExpenseRepository = FakeExpenseRepository(
        listOf(
            Expense("exp_1", 1200.0, "Food", "DISCRETIONARY", "Card", 1_717_043_200_000),
            Expense("exp_2", 800.0, "Rent", "COMMITTED", "Transfer", 1_717_129_600_000)
        )
    )

    private fun emptyRepository(): ExpenseRepository = FakeExpenseRepository(emptyList())

    @Test
    fun initialState_isEmpty() = testScope.runTest {
        val viewModel = ExpenseViewModel(emptyRepository())

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.expenses.isEmpty())
            assertEquals(0.0, state.committedTotal, 0.0)
            assertEquals(0.0, state.discretionaryTotal, 0.0)
        }
    }

    @Test
    fun addExpense_updatesState() = testScope.runTest {
        val viewModel = ExpenseViewModel(emptyRepository())
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
        val viewModel = ExpenseViewModel(seededRepository())
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
            withTimeout(1_000) { awaitItem() }
            val updated = withTimeout(1_000) { awaitItem() }
            assertEquals(2800.0, updated.committedTotal, 0.01)
            assertEquals(1700.0, updated.discretionaryTotal, 0.01)
        }
    }
}

