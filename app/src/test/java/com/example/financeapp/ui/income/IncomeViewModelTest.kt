package com.example.financeapp.ui.income

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.repository.IncomeRepository
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
class IncomeViewModelTest {
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

    private class FakeIncomeRepository(
        seed: List<Income>
    ) : IncomeRepository {
        private val entries = seed.toMutableList()

        override suspend fun insertIncome(income: Income) {
            entries.add(income)
        }

        override suspend fun updateIncome(income: Income) {
            val index = entries.indexOfFirst { it.id == income.id }
            if (index >= 0) {
                entries[index] = income
            }
        }

        override suspend fun deleteIncome(id: String) {
            entries.removeAll { it.id == id }
        }

        override suspend fun getAllIncomes(): List<Income> = entries.toList()

        override suspend fun getBySourceType(sourceType: String): List<Income> =
            entries.filter { it.sourceType == sourceType }

        override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
            entries.filter { it.date in startInclusive..endInclusive }.sumOf { it.amountLKR }
    }

    private fun seededRepository(): IncomeRepository {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        return FakeIncomeRepository(
            listOf(
                Income("inc_salary_01", 4200.0, "USD", 1_260_000.0, "SALARY", null, now - 6 * dayMillis),
                Income("inc_freelance_01", 500.0, "USD", 150_000.0, "FREELANCE", null, now - 4 * dayMillis),
                Income("inc_adsense_01", 150.0, "USD", 45_000.0, "ADSENSE", null, now - 2 * dayMillis),
                Income("inc_crypto_01", 0.05, "ETH", 90_000.0, "CRYPTO", null, now - dayMillis)
            )
        )
    }

    private fun emptyRepository(): IncomeRepository = FakeIncomeRepository(emptyList())

    @Test
    fun loadIncomeHistory_emitsLoadingThenSuccess() = testScope.runTest {
        val viewModel = IncomeViewModel(seededRepository())

        viewModel.state.test {
            assertEquals(IncomeUiState.Loading, awaitItem())
            viewModel.loadIncomeHistory()
            testScope.testScheduler.advanceUntilIdle()
            val nextState = withTimeout(1_000) { awaitItem() }
            assertTrue(nextState is IncomeUiState.Success)
        }
    }

    @Test
    fun totalLkr_updatesAfterLoading() = testScope.runTest {
        val viewModel = IncomeViewModel(seededRepository())

        viewModel.totalLkr.test {
            assertEquals(0.0, awaitItem(), 0.0)
            viewModel.loadIncomeHistory()
            testScope.testScheduler.advanceUntilIdle()
            val updated = withTimeout(1_000) { awaitItem() }
            assertEquals(1545000.0, updated, 0.01)
        }
    }

    @Test
    fun addIncome_persistsAndUpdatesTotals() = testScope.runTest {
        val viewModel = IncomeViewModel(emptyRepository())

        viewModel.totalLkr.test {
            assertEquals(0.0, awaitItem(), 0.0)
            viewModel.addIncome(
                amount = 100.0,
                currency = "USD",
                sourceType = "FREELANCE",
                sourceLabel = null,
                notes = null
            )
            testScope.testScheduler.advanceUntilIdle()
            val updated = withTimeout(1_000) { awaitItem() }
            assertEquals(30_000.0, updated, 0.01)
        }
    }
}
