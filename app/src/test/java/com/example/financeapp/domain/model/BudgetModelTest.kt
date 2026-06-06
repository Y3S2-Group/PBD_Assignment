package com.example.financeapp.domain.model

import org.junit.Assert.*
import org.junit.Test

class BudgetModelTest {

    // ── Budget ────────────────────────────────────────────────────────────────

    @Test
    fun budget_createsWithCorrectFields() {
        val budget = Budget(
            id = "budget_001",
            monthYear = "2024-06",
            categoryAllocationsJson = "{}",
            expectedIncome = 100_000.0
        )
        assertEquals("budget_001", budget.id)
        assertEquals("2024-06", budget.monthYear)
        assertEquals("{}", budget.categoryAllocationsJson)
        assertEquals(100_000.0, budget.expectedIncome, 0.0)
    }

    @Test
    fun budget_equality_trueWhenFieldsMatch() {
        val a = Budget("b1", "2024-01", "{}", 80_000.0)
        val b = Budget("b1", "2024-01", "{}", 80_000.0)
        assertEquals(a, b)
    }

    @Test
    fun budget_copy_updatesExpectedIncome() {
        val original = Budget("b1", "2024-06", "{}", 80_000.0)
        val updated = original.copy(expectedIncome = 100_000.0)
        assertEquals(100_000.0, updated.expectedIncome, 0.0)
        assertEquals("2024-06", updated.monthYear)
    }

    @Test
    fun budget_inequality_whenMonthYearDiffers() {
        val a = Budget("b1", "2024-01", "{}", 80_000.0)
        val b = Budget("b1", "2024-02", "{}", 80_000.0)
        assertNotEquals(a, b)
    }

    // ── BudgetCategory ────────────────────────────────────────────────────────

    @Test
    fun budgetCategory_createsWithCorrectFields() {
        val category = BudgetCategory(
            id = "2024-06_food",
            categoryName = "Food",
            allocatedAmount = 15_000.0,
            monthYear = "2024-06"
        )
        assertEquals("2024-06_food", category.id)
        assertEquals("Food", category.categoryName)
        assertEquals(15_000.0, category.allocatedAmount, 0.0)
        assertEquals("2024-06", category.monthYear)
    }

    @Test
    fun budgetCategory_equality_trueWhenFieldsMatch() {
        val a = BudgetCategory("id1", "Rent", 50_000.0, "2024-06")
        val b = BudgetCategory("id1", "Rent", 50_000.0, "2024-06")
        assertEquals(a, b)
    }

    @Test
    fun budgetCategory_copy_updatesAllocatedAmount() {
        val original = BudgetCategory("id1", "Food", 10_000.0, "2024-06")
        val updated = original.copy(allocatedAmount = 20_000.0)
        assertEquals(20_000.0, updated.allocatedAmount, 0.0)
        assertEquals("Food", updated.categoryName)
    }

    @Test
    fun budgetCategory_totalAllocated_sumIsCorrect() {
        val categories = listOf(
            BudgetCategory("id1", "Food", 15_000.0, "2024-06"),
            BudgetCategory("id2", "Rent", 50_000.0, "2024-06"),
            BudgetCategory("id3", "Transport", 8_000.0, "2024-06")
        )
        assertEquals(73_000.0, categories.sumOf { it.allocatedAmount }, 0.0)
    }

    // ── SavingsDeposit ────────────────────────────────────────────────────────

    @Test
    fun savingsDeposit_createsWithCorrectFields() {
        val deposit = SavingsDeposit(
            id = "dep_001",
            goalId = "goal_001",
            amount = 25_000.0,
            timestamp = 1_717_000_000_000L
        )
        assertEquals("dep_001", deposit.id)
        assertEquals("goal_001", deposit.goalId)
        assertEquals(25_000.0, deposit.amount, 0.0)
        assertEquals(1_717_000_000_000L, deposit.timestamp)
    }

    @Test
    fun savingsDeposit_equality_trueWhenFieldsMatch() {
        val a = SavingsDeposit("d1", "g1", 10_000.0, 0L)
        val b = SavingsDeposit("d1", "g1", 10_000.0, 0L)
        assertEquals(a, b)
    }

    @Test
    fun savingsDeposit_totalForGoal_sumIsCorrect() {
        val deposits = listOf(
            SavingsDeposit("d1", "goal_1", 10_000.0, 1_000L),
            SavingsDeposit("d2", "goal_1", 15_000.0, 2_000L),
            SavingsDeposit("d3", "goal_2", 5_000.0, 3_000L)
        )
        val goalTotal = deposits.filter { it.goalId == "goal_1" }.sumOf { it.amount }
        assertEquals(25_000.0, goalTotal, 0.0)
    }

    @Test
    fun savingsDeposit_filterByGoalId_returnsCorrectSubset() {
        val deposits = listOf(
            SavingsDeposit("d1", "goal_A", 10_000.0, 0L),
            SavingsDeposit("d2", "goal_B", 5_000.0, 0L),
            SavingsDeposit("d3", "goal_A", 8_000.0, 0L)
        )
        val forGoalA = deposits.filter { it.goalId == "goal_A" }
        assertEquals(2, forGoalA.size)
        assertEquals(18_000.0, forGoalA.sumOf { it.amount }, 0.0)
    }
}
