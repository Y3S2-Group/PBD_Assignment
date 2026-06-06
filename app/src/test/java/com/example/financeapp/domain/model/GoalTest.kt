package com.example.financeapp.domain.model

import org.junit.Assert.*
import org.junit.Test

class GoalTest {

    @Test
    fun goal_createsWithCorrectFields() {
        val goal = Goal(
            id = "goal_001",
            name = "New Laptop",
            targetAmount = 150_000.0,
            currentSavings = 0.0,
            currency = "LKR",
            deadlineTimestamp = 1_800_000_000_000L,
            createdAt = 1_700_000_000_000L
        )
        assertEquals("goal_001", goal.id)
        assertEquals("New Laptop", goal.name)
        assertEquals(150_000.0, goal.targetAmount, 0.0)
        assertEquals(0.0, goal.currentSavings, 0.0)
        assertEquals("LKR", goal.currency)
        assertEquals(1_800_000_000_000L, goal.deadlineTimestamp)
        assertEquals(1_700_000_000_000L, goal.createdAt)
    }

    @Test
    fun goal_currency_defaultsToLkr() {
        val goal = Goal("g", "Trip", 50_000.0, 0.0, deadlineTimestamp = 0L, createdAt = 0L)
        assertEquals("LKR", goal.currency)
    }

    @Test
    fun goal_equality_trueWhenFieldsMatch() {
        val a = Goal("g1", "Holiday", 80_000.0, 10_000.0, "LKR", 0L, 0L)
        val b = Goal("g1", "Holiday", 80_000.0, 10_000.0, "LKR", 0L, 0L)
        assertEquals(a, b)
    }

    @Test
    fun goal_inequality_whenIdDiffers() {
        val a = Goal("g1", "Holiday", 80_000.0, 0.0, "LKR", 0L, 0L)
        val b = Goal("g2", "Holiday", 80_000.0, 0.0, "LKR", 0L, 0L)
        assertNotEquals(a, b)
    }

    @Test
    fun goal_copy_updatesCurrentSavings() {
        val original = Goal("g", "Car", 1_000_000.0, 0.0, deadlineTimestamp = 0L, createdAt = 0L)
        val updated = original.copy(currentSavings = 250_000.0)
        assertEquals(250_000.0, updated.currentSavings, 0.0)
        assertEquals(1_000_000.0, updated.targetAmount, 0.0)
    }

    @Test
    fun goal_progressRatio_calculatesCorrectly() {
        val goal = Goal("g", "X", 100_000.0, 30_000.0, deadlineTimestamp = 0L, createdAt = 0L)
        val progress = (goal.currentSavings / goal.targetAmount) * 100.0
        assertEquals(30.0, progress, 0.001)
    }

    @Test
    fun goal_savingsAccumulation_viaCopy() {
        val goal = Goal("g", "X", 100_000.0, 0.0, deadlineTimestamp = 0L, createdAt = 0L)
        val afterFirstDeposit = goal.copy(currentSavings = goal.currentSavings + 20_000.0)
        val afterSecondDeposit = afterFirstDeposit.copy(currentSavings = afterFirstDeposit.currentSavings + 15_000.0)
        assertEquals(35_000.0, afterSecondDeposit.currentSavings, 0.0)
    }

    @Test
    fun goal_isCompleted_whenSavingsReachTarget() {
        val goal = Goal("g", "X", 50_000.0, 50_000.0, deadlineTimestamp = 0L, createdAt = 0L)
        assertTrue(goal.currentSavings >= goal.targetAmount)
    }

    @Test
    fun goal_remainingAmount_calculatesCorrectly() {
        val goal = Goal("g", "X", 120_000.0, 45_000.0, deadlineTimestamp = 0L, createdAt = 0L)
        val remaining = goal.targetAmount - goal.currentSavings
        assertEquals(75_000.0, remaining, 0.0)
    }

    @Test
    fun goal_usdCurrency_canBeStored() {
        val goal = Goal("g", "iPhone", 1_000.0, 0.0, currency = "USD", deadlineTimestamp = 0L, createdAt = 0L)
        assertEquals("USD", goal.currency)
        assertEquals(1_000.0, goal.targetAmount, 0.0)
    }

    @Test
    fun goal_zeroSavings_progressIsZero() {
        val goal = Goal("g", "X", 100_000.0, 0.0, deadlineTimestamp = 0L, createdAt = 0L)
        val progress = if (goal.targetAmount > 0) (goal.currentSavings / goal.targetAmount) * 100.0 else 0.0
        assertEquals(0.0, progress, 0.0)
    }
}
