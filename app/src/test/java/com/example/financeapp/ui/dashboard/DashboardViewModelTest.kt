package com.example.financeapp.ui.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.financeapp.domain.model.DashboardAnalytics
import com.example.financeapp.domain.model.DashboardInsight
import com.example.financeapp.domain.model.InsightTone
import com.example.financeapp.domain.repository.AnalyticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
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

    private class FakeAnalyticsRepository(
        private val summary: DashboardAnalytics,
        private val cached: DashboardAnalytics? = null,
        private val failRefresh: Boolean = false
    ) : AnalyticsRepository {
        override suspend fun buildDashboardSummary(nowMillis: Long): DashboardAnalytics = summary
        override suspend fun refreshDashboardSummary(nowMillis: Long): DashboardAnalytics {
            if (failRefresh) error("refresh failed")
            return summary
        }
        override suspend fun getCachedDashboardSummary(): DashboardAnalytics? = cached
    }

    @Test
    fun init_loadsDashboardSummary() = testScope.runTest {
        val viewModel = DashboardViewModel(
            repository = FakeAnalyticsRepository(
                DashboardAnalytics.empty().copy(
                    totalIncomeLkr = 125_000.0,
                    totalExpenseLkr = 42_000.0,
                    netSavingsLkr = 83_000.0,
                    insights = listOf(
                        DashboardInsight(
                            title = "Positive cash flow",
                            message = "You are saving more than you spend.",
                            tone = InsightTone.Positive
                        )
                    )
                )
            )
        )

        viewModel.state.test {
            assertTrue(awaitItem() is DashboardUiState.Loading)
            advanceUntilIdle()
            val loaded = awaitItem()
            assertTrue(loaded is DashboardUiState.Success)
            loaded as DashboardUiState.Success
            assertEquals(125_000.0, loaded.summary.totalIncomeLkr, 0.01)
            assertEquals(1, loaded.summary.insights.size)
        }
    }

    @Test
    fun loadDashboard_emitsCachedThenFreshSummary() = testScope.runTest {
        val cached = DashboardAnalytics.empty().copy(totalIncomeLkr = 10_000.0)
        val fresh = DashboardAnalytics.empty().copy(totalIncomeLkr = 25_000.0)
        val viewModel = DashboardViewModel(
            repository = FakeAnalyticsRepository(
                summary = fresh,
                cached = cached
            )
        )

        viewModel.state.test {
            assertTrue(awaitItem() is DashboardUiState.Loading)
            advanceUntilIdle()
            val cachedState = awaitItem()
            val freshState = awaitItem()
            assertEquals(10_000.0, (cachedState as DashboardUiState.Success).summary.totalIncomeLkr, 0.01)
            assertEquals(25_000.0, (freshState as DashboardUiState.Success).summary.totalIncomeLkr, 0.01)
        }
    }

    @Test
    fun loadDashboard_keepsCachedSummaryWhenRefreshFails() = testScope.runTest {
        val cached = DashboardAnalytics.empty().copy(totalIncomeLkr = 10_000.0)
        val viewModel = DashboardViewModel(
            repository = FakeAnalyticsRepository(
                summary = DashboardAnalytics.empty(),
                cached = cached,
                failRefresh = true
            )
        )

        viewModel.state.test {
            assertTrue(awaitItem() is DashboardUiState.Loading)
            advanceUntilIdle()
            val cachedState = awaitItem() as DashboardUiState.Success
            assertEquals(10_000.0, cachedState.summary.totalIncomeLkr, 0.01)
            expectNoEvents()
        }
    }
}
