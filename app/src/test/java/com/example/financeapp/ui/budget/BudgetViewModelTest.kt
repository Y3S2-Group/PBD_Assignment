package com.example.financeapp.ui.budget

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.SavingsDeposit
import com.example.financeapp.domain.repository.BudgetRepository
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // -------------------------------------------------------------------------
    // Fake repository
    // -------------------------------------------------------------------------

    private class FakeBudgetRepository(seedGoal: Goal? = null) : BudgetRepository {
        var storedGoal: Goal? = seedGoal
            private set
        val categories = mutableListOf<BudgetCategory>()
        val deposits = mutableListOf<SavingsDeposit>()
        var monthlyIncomeAverage: Double = 0.0

        override suspend fun upsertGoal(goal: Goal) {
            storedGoal = goal
        }

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

        override suspend fun getActualSpentByCategory(monthYear: String): Map<String, Double> =
            emptyMap()

        override suspend fun insertDeposit(deposit: SavingsDeposit) {
            deposits.add(deposit)
        }

        override suspend fun getDepositsForGoal(goalId: String): List<SavingsDeposit> =
            deposits.filter { it.goalId == goalId }

        override suspend fun getMonthlyIncomeAverage(monthsBack: Int): Double = monthlyIncomeAverage
    }

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private fun goalWithDeadlineMonthsFromNow(months: Int, savings: Double = 0.0): Goal {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val deadline = now.atZone(zone).plusMonths(months.toLong()).toInstant().toEpochMilli()
        return Goal(
            id = "goal_test",
            name = "Test Goal",
            targetAmount = 120_000.0,
            currentSavings = savings,
            currency = "LKR",
            deadlineTimestamp = deadline,
            createdAt = now.toEpochMilli()
        )
    }

    // -------------------------------------------------------------------------
    // calculateRequiredMonthlySavings
    // -------------------------------------------------------------------------

    @Test
    fun requiredMonthlySavings_isCorrect() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        val result = vm.calculateRequiredMonthlySavings(goal, monthsRemaining = 12)
        assertEquals(10_000.0, result, 0.01)
    }

    @Test
    fun requiredMonthlySavings_accountsForExistingSavings() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12, savings = 60_000.0)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        val result = vm.calculateRequiredMonthlySavings(goal, monthsRemaining = 6)
        assertEquals(10_000.0, result, 0.01)
    }

    @Test
    fun requiredMonthlySavings_returnsZero_whenNoMonthsLeft() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(0)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(0.0, vm.calculateRequiredMonthlySavings(goal, 0), 0.0)
    }

    // -------------------------------------------------------------------------
    // calculateProgressPercent
    // -------------------------------------------------------------------------

    @Test
    fun progressPercent_isCorrect() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12, savings = 30_000.0)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(25.0, vm.calculateProgressPercent(goal), 0.01)
    }

    @Test
    fun progressPercent_isZero_whenTargetIsZero() = testScope.runTest {
        val goal = Goal("g", "x", 0.0, 0.0, "LKR", 0L, 0L)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(0.0, vm.calculateProgressPercent(goal), 0.0)
    }

    // -------------------------------------------------------------------------
    // calculateGoalStatus
    // -------------------------------------------------------------------------

    @Test
    fun goalStatus_isAhead_whenSavingsExceedExpected() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val createdAt = now.atZone(zone).minusMonths(6).toInstant().toEpochMilli()
        val deadline = now.atZone(zone).plusMonths(6).toInstant().toEpochMilli()
        val goal = Goal(
            id = "g", name = "x", targetAmount = 120_000.0,
            currentSavings = 90_000.0,  // well ahead of expected 50%
            currency = "LKR", deadlineTimestamp = deadline, createdAt = createdAt
        )
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(GoalStatus.AHEAD, vm.calculateGoalStatus(goal))
    }

    @Test
    fun goalStatus_isBehind_whenSavingsTooLow() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val createdAt = now.atZone(zone).minusMonths(6).toInstant().toEpochMilli()
        val deadline = now.atZone(zone).plusMonths(6).toInstant().toEpochMilli()
        val goal = Goal(
            id = "g", name = "x", targetAmount = 120_000.0,
            currentSavings = 1_000.0,  // way behind expected ~60,000
            currency = "LKR", deadlineTimestamp = deadline, createdAt = createdAt
        )
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(GoalStatus.BEHIND, vm.calculateGoalStatus(goal))
    }

    @Test
    fun goalStatus_isOnTrack_whenSavingsNearExpected() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val createdAt = now.atZone(zone).minusMonths(6).toInstant().toEpochMilli()
        val deadline = now.atZone(zone).plusMonths(6).toInstant().toEpochMilli()
        val goal = Goal(
            id = "g", name = "x", targetAmount = 120_000.0,
            currentSavings = 60_000.0,  // exactly on track at 50% elapsed
            currency = "LKR", deadlineTimestamp = deadline, createdAt = createdAt
        )
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(GoalStatus.ON_TRACK, vm.calculateGoalStatus(goal))
    }

    // -------------------------------------------------------------------------
    // calculateDaysRemaining
    // -------------------------------------------------------------------------

    @Test
    fun daysRemaining_isPositive_forFutureDeadline() = testScope.runTest {
        val vm = BudgetViewModel(FakeBudgetRepository())
        val deadline = Instant.now().plusSeconds(30L * 24 * 60 * 60).toEpochMilli()
        val days = vm.calculateDaysRemaining(deadline)
        assertTrue(days in 28..31)
    }

    @Test
    fun daysRemaining_isZero_forPastDeadline() = testScope.runTest {
        val vm = BudgetViewModel(FakeBudgetRepository())
        val past = Instant.now().minusSeconds(1000).toEpochMilli()
        assertEquals(0, vm.calculateDaysRemaining(past))
    }

    // -------------------------------------------------------------------------
    // calculateMonthsEarlier
    // -------------------------------------------------------------------------

    @Test
    fun monthsEarlier_isPositive_withReduction() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        val earlier = vm.calculateMonthsEarlier(5_000.0, goal)
        assertTrue("Expected monthsEarlier > 0 but was $earlier", earlier > 0)
    }

    @Test
    fun monthsEarlier_isZero_withNoReduction() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(0, vm.calculateMonthsEarlier(0.0, goal))
    }

    @Test
    fun monthsEarlier_isZero_whenGoalAlreadyMet() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12, savings = 120_000.0)
        val vm = BudgetViewModel(FakeBudgetRepository(goal))
        assertEquals(0, vm.calculateMonthsEarlier(5_000.0, goal))
    }

    // -------------------------------------------------------------------------
    // addSavingsToGoal
    // -------------------------------------------------------------------------

    @Test
    fun addSavings_updatesGoalCurrentSavings() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val repo = FakeBudgetRepository(goal)
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        vm.addSavingsToGoal(25_000.0)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(25_000.0, vm.state.value.activeGoal?.currentSavings ?: 0.0, 0.01)
    }

    @Test
    fun addSavings_createsDepositRecord() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val repo = FakeBudgetRepository(goal)
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        vm.addSavingsToGoal(10_000.0)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(1, repo.deposits.size)
        assertEquals(10_000.0, repo.deposits.first().amount, 0.01)
        assertEquals(goal.id, repo.deposits.first().goalId)
    }

    @Test
    fun addSavings_ignoresZeroOrNegativeAmount() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val repo = FakeBudgetRepository(goal)
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        vm.addSavingsToGoal(0.0)
        vm.addSavingsToGoal(-500.0)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(0, repo.deposits.size)
    }

    // -------------------------------------------------------------------------
    // createGoal
    // -------------------------------------------------------------------------

    @Test
    fun createGoal_persistsGoalInRepository() = testScope.runTest {
        val repo = FakeBudgetRepository()
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        val deadline = Instant.now().plusSeconds(365L * 24 * 60 * 60).toEpochMilli()
        vm.createGoal("Laptop", 150_000.0, "LKR", deadline)
        testScope.testScheduler.advanceUntilIdle()

        assertNotNull(repo.storedGoal)
        assertEquals("Laptop", repo.storedGoal?.name)
        assertEquals(150_000.0, repo.storedGoal?.targetAmount ?: 0.0, 0.01)
    }

    @Test
    fun createGoal_convertsUsdToLkr() = testScope.runTest {
        val repo = FakeBudgetRepository()
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        val deadline = Instant.now().plusSeconds(365L * 24 * 60 * 60).toEpochMilli()
        vm.createGoal("iPhone", 1_000.0, "USD", deadline)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(300_000.0, repo.storedGoal?.targetAmount ?: 0.0, 0.01)
    }

    @Test
    fun createGoal_ignoresBlankName() = testScope.runTest {
        val repo = FakeBudgetRepository()
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        vm.createGoal("", 50_000.0, "LKR", System.currentTimeMillis() + 1_000_000)
        testScope.testScheduler.advanceUntilIdle()

        assertNull(repo.storedGoal)
    }

    // -------------------------------------------------------------------------
    // updateGoal
    // -------------------------------------------------------------------------

    @Test
    fun updateGoal_updatesNameAndAmount() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val repo = FakeBudgetRepository(goal)
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        val newDeadline = Instant.now().plusSeconds(365L * 24 * 60 * 60).toEpochMilli()
        vm.updateGoal("New Laptop", 200_000.0, "LKR", newDeadline)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals("New Laptop", repo.storedGoal?.name)
        assertEquals(200_000.0, repo.storedGoal?.targetAmount ?: 0.0, 0.01)
    }

    @Test
    fun updateGoal_convertsUsdToLkr() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val repo = FakeBudgetRepository(goal)
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        val newDeadline = Instant.now().plusSeconds(365L * 24 * 60 * 60).toEpochMilli()
        vm.updateGoal("iPhone", 1_000.0, "USD", newDeadline)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(300_000.0, repo.storedGoal?.targetAmount ?: 0.0, 0.01)
        assertEquals("USD", repo.storedGoal?.currency)
    }

    @Test
    fun updateGoal_ignoresBlankName() = testScope.runTest {
        val goal = goalWithDeadlineMonthsFromNow(12)
        val repo = FakeBudgetRepository(goal)
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        val originalName = goal.name
        val newDeadline = Instant.now().plusSeconds(365L * 24 * 60 * 60).toEpochMilli()
        vm.updateGoal("", 50_000.0, "LKR", newDeadline)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(originalName, repo.storedGoal?.name)
    }

    @Test
    fun updateGoal_doesNothing_whenNoActiveGoal() = testScope.runTest {
        val repo = FakeBudgetRepository()
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        val newDeadline = Instant.now().plusSeconds(365L * 24 * 60 * 60).toEpochMilli()
        vm.updateGoal("Something", 50_000.0, "LKR", newDeadline)
        testScope.testScheduler.advanceUntilIdle()

        assertNull(repo.storedGoal)
    }

    // -------------------------------------------------------------------------
    // setCategoryBudget
    // -------------------------------------------------------------------------

    @Test
    fun setCategoryBudget_savesCategory() = testScope.runTest {
        val repo = FakeBudgetRepository()
        val vm = BudgetViewModel(repo)

        testScope.testScheduler.advanceUntilIdle()
        vm.setCategoryBudget("Food", 15_000.0)
        testScope.testScheduler.advanceUntilIdle()

        val saved = repo.categories.find { it.categoryName == "Food" }
        assertNotNull(saved)
        assertEquals(15_000.0, saved?.allocatedAmount ?: 0.0, 0.01)
    }

    // -------------------------------------------------------------------------
    // computeSavingsStreakFromDeposits
    // -------------------------------------------------------------------------

    @Test
    fun streak_isZero_whenNoDeposits() = testScope.runTest {
        val vm = BudgetViewModel(FakeBudgetRepository())
        assertEquals(0, vm.computeSavingsStreakFromDeposits(emptyList(), 10_000.0))
    }

    @Test
    fun streak_isZero_whenRequiredIsZero() = testScope.runTest {
        val vm = BudgetViewModel(FakeBudgetRepository())
        val deposit = SavingsDeposit("d1", "g1", 5_000.0, Instant.now().toEpochMilli())
        assertEquals(0, vm.computeSavingsStreakFromDeposits(listOf(deposit), 0.0))
    }

    @Test
    fun streak_countsCurrentMonth_whenTargetMet() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val thisMonthStart = YearMonth.now().atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val deposit = SavingsDeposit("d1", "g1", 15_000.0, thisMonthStart + 1_000)
        val vm = BudgetViewModel(FakeBudgetRepository())
        val streak = vm.computeSavingsStreakFromDeposits(listOf(deposit), 10_000.0)
        assertEquals(1, streak)
    }

    @Test
    fun streak_isZero_whenCurrentMonthTargetNotMet() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val thisMonthStart = YearMonth.now().atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val deposit = SavingsDeposit("d1", "g1", 5_000.0, thisMonthStart + 1_000)
        val vm = BudgetViewModel(FakeBudgetRepository())
        val streak = vm.computeSavingsStreakFromDeposits(listOf(deposit), 10_000.0)
        assertEquals(0, streak)
    }

    @Test
    fun streak_countsConsecutiveMonths() = testScope.runTest {
        val zone = ZoneId.systemDefault()
        val now = YearMonth.now()
        fun depositForMonth(month: YearMonth, amount: Double): SavingsDeposit {
            val ts = month.atDay(15).atStartOfDay(zone).toInstant().toEpochMilli()
            return SavingsDeposit("d_${month}", "g1", amount, ts)
        }
        val deposits = listOf(
            depositForMonth(now, 12_000.0),
            depositForMonth(now.minusMonths(1), 11_000.0),
            depositForMonth(now.minusMonths(2), 10_000.0),
            depositForMonth(now.minusMonths(3), 3_000.0)  // breaks streak
        )
        val vm = BudgetViewModel(FakeBudgetRepository())
        val streak = vm.computeSavingsStreakFromDeposits(deposits, 10_000.0)
        assertEquals(3, streak)
    }

    // -------------------------------------------------------------------------
    // state after refresh with no goal
    // -------------------------------------------------------------------------

    @Test
    fun state_hasNullGoal_whenRepositoryIsEmpty() = testScope.runTest {
        val vm = BudgetViewModel(FakeBudgetRepository())
        testScope.testScheduler.advanceUntilIdle()
        assertNull(vm.state.value.activeGoal)
    }

    @Test
    fun state_reflectsMonthlyIncomeAverage() = testScope.runTest {
        val repo = FakeBudgetRepository()
        repo.monthlyIncomeAverage = 80_000.0
        val vm = BudgetViewModel(repo)
        testScope.testScheduler.advanceUntilIdle()
        assertEquals(80_000.0, vm.state.value.monthlyIncomeAverage, 0.01)
    }
}
