package com.example.financeapp.ui.income

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
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

    @Test
    fun loadIncomeHistory_emitsLoadingThenSuccess() = testScope.runTest {
        val viewModel = IncomeViewModel()

        viewModel.state.test {
            assertEquals(IncomeUiState.Loading, awaitItem())
            viewModel.loadIncomeHistory()
            val nextState = withTimeout(1_000) { awaitItem() }
            assertTrue(nextState is IncomeUiState.Success)
        }
    }

    @Test
    fun totalLkr_updatesAfterLoading() = testScope.runTest {
        val viewModel = IncomeViewModel()

        viewModel.totalLkr.test {
            assertEquals(0.0, awaitItem(), 0.0)
            viewModel.loadIncomeHistory()
            val updated = withTimeout(1_000) { awaitItem() }
            assertEquals(1545000.0, updated, 0.01)
        }
    }
}
