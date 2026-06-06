package com.example.financeapp.ui.budget

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.SavingsDeposit
import com.example.financeapp.domain.repository.BudgetRepository
import com.example.financeapp.util.AppEventBus
import io.mockk.mockk
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetCalculationsTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val fakeEventBus = AppEventBus()
    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBudgetRepository(seedGoal: Goal? = null) : BudgetRepository {
        var storedGoal: Goal? = seedGoal
            private set
        val categories = mutableListOf<BudgetCategory>()
        val deposits = mutableListOf<SavingsDeposit>()
        var monthlyIncomeAverage: Double = 0.0

        override suspend fun upsertGoal(goal: Goal) { storedGoal = goal }
        override suspend fun getLatestGoal(): Goal? = storedGoal
        override suspend fun updateGoalSavings(id: String, newSavings: Double) {
            storedGoal = storedGoal?.copy(currentSavings = newSavings)
        }
        override suspend fun upsertBudgetCategory(category: BudgetCategory) {
            categories.removeAll { it.id == category.id }
            categories.add(category)
        }
        override suspend fun getBudgetCategoriesForMonth(monthYear: String): List<BudgetCategory> =
            categories.filter { it.monthYear == monthYear }
        override suspend fun getActualSpentByCategory(monthYear: String): Map<String, Double> = emptyMap()
        override suspend fun insertDeposit(deposit: SavingsDeposit) { deposits.add(deposit) }
        override suspend fun getDepositsForGoal(goalId: String): List<SavingsDeposit> =
            deposits.filter { it.goalId == goalId }
        override suspend fun getMonthlyIncomeAverage(monthsBack: Int): Double = monthlyIncomeAverage
    }

    private fun makeVm(repo: FakeBudgetRepository = FakeBudgetRepository()): BudgetViewModel =
        BudgetViewModel(repo, fakeEventBus, mockContext)

    private fun goalWithMonthsFromNow(months: Int, savings: Double = 0.0): Goal {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val deadline = now.atZone(zone).plusMonths(months.toLong()).toInstant().toEpochMilli()
        return Goal("goal_test", "Test Goal", 120_000.0, savings, "LKR", deadline, now.toEpochMilli())
    }

    // ── calculateRequiredMonthlySavings ───────────────────────────────────────

    @Test
    fun requiredMonthlySavings_dividesRemainingByMonths() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12)
        val vm = makeVm()
        assertEquals(10_000.0, vm.calculateRequiredMonthlySavings(goal, 12), 0.01)
    }

    @Test
    fun requiredMonthlySavings_accountsForExistingSavings() = testScope.runTest {
        val goal = goalWithMonthsFromNow(6, savings = 60_000.0)
        val vm = makeVm()
        assertEquals(10_000.0, vm.calculateRequiredMonthlySavings(goal, 6), 0.01)
    }

    @Test
    fun requiredMonthlySavings_returnsZero_whenNoMonthsLeft() = testScope.runTest {
        val goal = goalWithMonthsFromNow(0)
        val vm = makeVm()
        assertEquals(0.0, vm.calculateRequiredMonthlySavings(goal, 0), 0.0)
    }

    @Test
    fun requiredMonthlySavings_returnsZero_whenGoalAlreadyMet() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12, savings = 120_000.0)
        val vm = makeVm()
        assertEquals(0.0, vm.calculateRequiredMonthlySavings(goal, 12), 0.01)
    }

    // ── calculateProgressPercent ──────────────────────────────────────────────

    @Test
    fun progressPercent_calculatesCorrectly() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12, savings = 30_000.0)
        val vm = makeVm()
        assertEquals(25.0, vm.calculateProgressPercent(goal), 0.01)
    }

    @Test
    fun progressPercent_isZero_whenTargetIsZero() = testScope.runTest {
        val goal = Goal("g", "x", 0.0, 0.0, "LKR", 0L, 0L)
        val vm = makeVm()
        assertEquals(0.0, vm.calculateProgressPercent(goal), 0.0)
    }

    @Test
    fun progressPercent_isOneHundred_whenFullySaved() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12, savings = 120_000.0)
        val vm = makeVm()
        assertEquals(100.0, vm.calculateProgressPercent(goal), 0.01)
    }

    @Test
    fun progressPercent_isPartial_forHalfSaved() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12, savings = 60_000.0)
        val vm = makeVm()
        assertEquals(50.0, vm.calculateProgressPercent(goal), 0.01)
    }

    // ── calculateGoalStatus ───────────────────────────────────────────────────

    @Test
    fun goalStatus_isAhead_whenSavingsExceedExpected() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val createdAt = now.atZone(zone).minusMonths(6).toInstant().toEpochMilli()
        val deadline = now.atZone(zone).plusMonths(6).toInstant().toEpochMilli()
        val goal = Goal("g", "x", 120_000.0, 90_000.0, "LKR", deadline, createdAt)
        val vm = makeVm()
        assertEquals(GoalStatus.AHEAD, vm.calculateGoalStatus(goal))
    }

    @Test
    fun goalStatus_isBehind_whenSavingsTooLow() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val createdAt = now.atZone(zone).minusMonths(6).toInstant().toEpochMilli()
        val deadline = now.atZone(zone).plusMonths(6).toInstant().toEpochMilli()
        val goal = Goal("g", "x", 120_000.0, 1_000.0, "LKR", deadline, createdAt)
        val vm = makeVm()
        assertEquals(GoalStatus.BEHIND, vm.calculateGoalStatus(goal))
    }

    @Test
    fun goalStatus_isOnTrack_whenSavingsMatchExpected() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val createdAt = now.atZone(zone).minusMonths(6).toInstant().toEpochMilli()
        val deadline = now.atZone(zone).plusMonths(6).toInstant().toEpochMilli()
        val goal = Goal("g", "x", 120_000.0, 60_000.0, "LKR", deadline, createdAt)
        val vm = makeVm()
        assertEquals(GoalStatus.ON_TRACK, vm.calculateGoalStatus(goal))
    }

    @Test
    fun goalStatus_isOnTrack_whenNoTimeHasElapsed() = testScope.runTest {
        val now = Instant.now()
        val goal = Goal("g", "x", 120_000.0, 0.0, "LKR",
            now.plusSeconds(365L * 24 * 3600).toEpochMilli(), now.toEpochMilli())
        val vm = makeVm()
        assertEquals(GoalStatus.ON_TRACK, vm.calculateGoalStatus(goal))
    }

    // ── calculateDaysRemaining ────────────────────────────────────────────────

    @Test
    fun daysRemaining_isPositive_forFutureDeadline() = testScope.runTest {
        val vm = makeVm()
        val deadline = Instant.now().plusSeconds(30L * 24 * 60 * 60).toEpochMilli()
        val days = vm.calculateDaysRemaining(deadline)
        assertTrue("Expected 28..31 but was $days", days in 28..31)
    }

    @Test
    fun daysRemaining_isZero_forPastDeadline() = testScope.runTest {
        val vm = makeVm()
        assertEquals(0, vm.calculateDaysRemaining(Instant.now().minusSeconds(1000).toEpochMilli()))
    }

    @Test
    fun daysRemaining_isZero_forZeroTimestamp() = testScope.runTest {
        val vm = makeVm()
        assertEquals(0, vm.calculateDaysRemaining(0L))
    }

    @Test
    fun daysRemaining_isApproxOneYear_forYearAhead() = testScope.runTest {
        val vm = makeVm()
        val deadline = Instant.now().plusSeconds(365L * 24 * 3600).toEpochMilli()
        val days = vm.calculateDaysRemaining(deadline)
        assertTrue("Expected ~365 but was $days", days in 363..367)
    }

    // ── calculateMonthsEarlier ────────────────────────────────────────────────

    @Test
    fun monthsEarlier_isPositive_withExtraContribution() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12)
        val vm = makeVm()
        val earlier = vm.calculateMonthsEarlier(5_000.0, goal)
        assertTrue("Expected > 0 but was $earlier", earlier > 0)
    }

    @Test
    fun monthsEarlier_isZero_withZeroContribution() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12)
        val vm = makeVm()
        assertEquals(0, vm.calculateMonthsEarlier(0.0, goal))
    }

    @Test
    fun monthsEarlier_isZero_whenGoalAlreadyMet() = testScope.runTest {
        val goal = goalWithMonthsFromNow(12, savings = 120_000.0)
        val vm = makeVm()
        assertEquals(0, vm.calculateMonthsEarlier(5_000.0, goal))
    }

    // ── computeSavingsStreakFromDeposits ──────────────────────────────────────

    @Test
    fun streak_isZero_withNoDeposits() = testScope.runTest {
        val vm = makeVm()
        assertEquals(0, vm.computeSavingsStreakFromDeposits(emptyList(), 10_000.0))
    }

    @Test
    fun streak_isZero_whenRequiredIsZero() = testScope.runTest {
        val vm = makeVm()
        val deposit = SavingsDeposit("d1", "g1", 5_000.0, Instant.now().toEpochMilli())
        assertEquals(0, vm.computeSavingsStreakFromDeposits(listOf(deposit), 0.0))
    }

    @Test
    fun streak_isOne_whenCurrentMonthTargetMet() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val thisMonthStart = YearMonth.now().atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val deposit = SavingsDeposit("d1", "g1", 15_000.0, thisMonthStart + 1_000)
        val vm = makeVm()
        assertEquals(1, vm.computeSavingsStreakFromDeposits(listOf(deposit), 10_000.0))
    }

    @Test
    fun streak_isZero_whenCurrentMonthTargetNotMet() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val thisMonthStart = YearMonth.now().atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val deposit = SavingsDeposit("d1", "g1", 5_000.0, thisMonthStart + 1_000)
        val vm = makeVm()
        assertEquals(0, vm.computeSavingsStreakFromDeposits(listOf(deposit), 10_000.0))
    }

    @Test
    fun streak_countsConsecutiveMonths_andBreaksOnMiss() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = YearMonth.now()
        fun depositForMonth(month: YearMonth, amount: Double): SavingsDeposit {
            val ts = month.atDay(15).atStartOfDay(zone).toInstant().toEpochMilli()
            return SavingsDeposit("d_$month", "g1", amount, ts)
        }
        val deposits = listOf(
            depositForMonth(now, 12_000.0),
            depositForMonth(now.minusMonths(1), 11_000.0),
            depositForMonth(now.minusMonths(2), 10_000.0),
            depositForMonth(now.minusMonths(3), 3_000.0)   // breaks streak
        )
        val vm = makeVm()
        assertEquals(3, vm.computeSavingsStreakFromDeposits(deposits, 10_000.0))
    }

    @Test
    fun streak_countsOnlyOne_withJustCurrentMonth() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = YearMonth.now()
        val ts = now.atDay(10).atStartOfDay(zone).toInstant().toEpochMilli()
        val deposit = SavingsDeposit("d1", "g1", 20_000.0, ts)
        val vm = makeVm()
        assertEquals(1, vm.computeSavingsStreakFromDeposits(listOf(deposit), 10_000.0))
    }

    // ── calculateMonthsRemaining ──────────────────────────────────────────────

    @Test
    fun monthsRemaining_isApproxTwelve_forYearFromNow() = testScope.runTest {
        val vm = makeVm()
        val goal = goalWithMonthsFromNow(12)
        val months = vm.calculateMonthsRemaining(goal.deadlineTimestamp)
        assertTrue("Expected ~12 but was $months", months in 11..13)
    }

    @Test
    fun monthsRemaining_isZero_forPastDeadline() = testScope.runTest {
        val vm = makeVm()
        assertEquals(0, vm.calculateMonthsRemaining(Instant.now().minusSeconds(1000).toEpochMilli()))
    }

    @Test
    fun monthsRemaining_isZero_forZeroTimestamp() = testScope.runTest {
        val vm = makeVm()
        assertEquals(0, vm.calculateMonthsRemaining(0L))
    }
}
