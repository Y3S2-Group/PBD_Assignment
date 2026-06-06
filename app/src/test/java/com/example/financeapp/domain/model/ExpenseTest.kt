package com.example.financeapp.domain.model

import org.junit.Assert.*
import org.junit.Test

class ExpenseTest {

    @Test
    fun expense_createsWithCorrectFields() {
        val expense = Expense(
            id = "exp_001",
            amountLkr = 1500.0,
            category = "Food",
            spendingType = "DISCRETIONARY",
            paymentMethod = "Card",
            timestamp = 1_717_000_000_000L
        )
        assertEquals("exp_001", expense.id)
        assertEquals(1500.0, expense.amountLkr, 0.0)
        assertEquals("Food", expense.category)
        assertEquals("DISCRETIONARY", expense.spendingType)
        assertEquals("Card", expense.paymentMethod)
        assertEquals(1_717_000_000_000L, expense.timestamp)
    }

    @Test
    fun expense_equality_trueWhenFieldsMatch() {
        val a = Expense("exp_1", 500.0, "Food", "DISCRETIONARY", "Card", 0L)
        val b = Expense("exp_1", 500.0, "Food", "DISCRETIONARY", "Card", 0L)
        assertEquals(a, b)
    }

    @Test
    fun expense_inequality_whenIdDiffers() {
        val a = Expense("exp_1", 500.0, "Food", "DISCRETIONARY", "Card", 0L)
        val b = Expense("exp_2", 500.0, "Food", "DISCRETIONARY", "Card", 0L)
        assertNotEquals(a, b)
    }

    @Test
    fun expense_copy_updatesAmountOnly() {
        val original = Expense("exp_1", 500.0, "Food", "DISCRETIONARY", "Cash", 0L)
        val updated = original.copy(amountLkr = 1000.0)
        assertEquals(1000.0, updated.amountLkr, 0.0)
        assertEquals("Food", updated.category)
        assertEquals("exp_1", updated.id)
    }

    @Test
    fun expense_committedSpendingType_isStored() {
        val expense = Expense("exp_1", 10_000.0, "Rent", "COMMITTED", "Transfer", 0L)
        assertEquals("COMMITTED", expense.spendingType)
    }

    @Test
    fun expense_sum_committedTotal_isCorrect() {
        val expenses = listOf(
            Expense("e1", 10_000.0, "Rent", "COMMITTED", "Transfer", 0L),
            Expense("e2", 5_000.0, "Insurance", "COMMITTED", "Card", 0L),
            Expense("e3", 2_000.0, "Coffee", "DISCRETIONARY", "Cash", 0L)
        )
        val committed = expenses.filter { it.spendingType == "COMMITTED" }.sumOf { it.amountLkr }
        assertEquals(15_000.0, committed, 0.0)
    }

    @Test
    fun expense_sum_discretionaryTotal_isCorrect() {
        val expenses = listOf(
            Expense("e1", 10_000.0, "Rent", "COMMITTED", "Transfer", 0L),
            Expense("e2", 1_500.0, "Food", "DISCRETIONARY", "Card", 0L),
            Expense("e3", 800.0, "Gym", "DISCRETIONARY", "Card", 0L)
        )
        val discretionary = expenses.filter { it.spendingType == "DISCRETIONARY" }.sumOf { it.amountLkr }
        assertEquals(2_300.0, discretionary, 0.0)
    }

    @Test
    fun expense_filterByCategory_returnsCorrectSubset() {
        val expenses = listOf(
            Expense("e1", 1_200.0, "Food", "DISCRETIONARY", "Card", 0L),
            Expense("e2", 800.0, "Transport", "DISCRETIONARY", "Cash", 0L),
            Expense("e3", 600.0, "Food", "DISCRETIONARY", "Card", 0L)
        )
        val food = expenses.filter { it.category == "Food" }
        assertEquals(2, food.size)
        assertEquals(1_800.0, food.sumOf { it.amountLkr }, 0.0)
    }

    @Test
    fun expense_sortByTimestamp_descending() {
        val expenses = listOf(
            Expense("e1", 100.0, "Food", "DISCRETIONARY", "Card", 1_000L),
            Expense("e2", 200.0, "Rent", "COMMITTED", "Transfer", 3_000L),
            Expense("e3", 150.0, "Gym", "DISCRETIONARY", "Card", 2_000L)
        )
        val sorted = expenses.sortedByDescending { it.timestamp }
        assertEquals("e2", sorted[0].id)
        assertEquals("e3", sorted[1].id)
        assertEquals("e1", sorted[2].id)
    }

    @Test
    fun expense_groupByCategory_sumsByCategory() {
        val expenses = listOf(
            Expense("e1", 1_000.0, "Food", "DISCRETIONARY", "Card", 0L),
            Expense("e2", 800.0, "Food", "DISCRETIONARY", "Cash", 0L),
            Expense("e3", 5_000.0, "Rent", "COMMITTED", "Transfer", 0L)
        )
        val byCategory = expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amountLkr } }
        assertEquals(1_800.0, byCategory["Food"] ?: 0.0, 0.0)
        assertEquals(5_000.0, byCategory["Rent"] ?: 0.0, 0.0)
    }
}
