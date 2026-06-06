package com.example.financeapp.domain.model

import org.junit.Assert.*
import org.junit.Test

class IncomeTest {

    @Test
    fun income_createsWithCorrectDefaults() {
        val income = Income(
            id = "inc_001",
            amount = 1000.0,
            currency = "USD",
            amountLKR = 300_000.0,
            sourceType = "SALARY",
            date = 1_717_000_000_000L
        )
        assertEquals("inc_001", income.id)
        assertEquals(1000.0, income.amount, 0.0)
        assertEquals("USD", income.currency)
        assertEquals(300_000.0, income.amountLKR, 0.0)
        assertEquals("SALARY", income.sourceType)
        assertNull(income.sourceLabel)
        assertNull(income.notes)
        assertNull(income.projectRef)
        assertEquals(1.0, income.exchangeRate, 0.0)
        assertFalse(income.isRecurring)
        assertTrue(income.invoicePaid)
    }

    @Test
    fun income_equality_trueWhenFieldsMatch() {
        val a = Income("inc_1", 500.0, "LKR", 500.0, "SALARY", date = 0L)
        val b = Income("inc_1", 500.0, "LKR", 500.0, "SALARY", date = 0L)
        assertEquals(a, b)
    }

    @Test
    fun income_inequality_whenIdDiffers() {
        val a = Income("inc_1", 500.0, "LKR", 500.0, "SALARY", date = 0L)
        val b = Income("inc_2", 500.0, "LKR", 500.0, "SALARY", date = 0L)
        assertNotEquals(a, b)
    }

    @Test
    fun income_copy_updatesAmountOnly() {
        val original = Income("inc_1", 500.0, "LKR", 500.0, "FREELANCE", date = 0L)
        val updated = original.copy(amount = 750.0)
        assertEquals(750.0, updated.amount, 0.0)
        assertEquals("inc_1", updated.id)
        assertEquals("FREELANCE", updated.sourceType)
    }

    @Test
    fun income_amountLkr_isStoredSeparatelyFromAmount() {
        val income = Income("i", 100.0, "USD", 30_000.0, "ADSENSE", date = 0L)
        assertEquals(30_000.0, income.amountLKR, 0.0)
        assertEquals(100.0, income.amount, 0.0)
    }

    @Test
    fun income_isRecurring_canBeSetTrue() {
        val income = Income("i", 200.0, "LKR", 200.0, "SALARY", date = 0L, isRecurring = true)
        assertTrue(income.isRecurring)
    }

    @Test
    fun income_invoicePaid_defaultsToTrue() {
        val income = Income("i", 200.0, "LKR", 200.0, "FREELANCE", date = 0L)
        assertTrue(income.invoicePaid)
    }

    @Test
    fun income_invoicePaid_canBeSetFalse() {
        val income = Income("i", 200.0, "USD", 60_000.0, "FREELANCE", date = 0L, invoicePaid = false)
        assertFalse(income.invoicePaid)
    }

    @Test
    fun income_sum_ofAmountLkrListIsCorrect() {
        val incomes = listOf(
            Income("i1", 1000.0, "LKR", 1000.0, "SALARY", date = 0L),
            Income("i2", 2000.0, "LKR", 2000.0, "FREELANCE", date = 0L),
            Income("i3", 500.0, "LKR", 500.0, "ADSENSE", date = 0L)
        )
        assertEquals(3500.0, incomes.sumOf { it.amountLKR }, 0.0)
    }

    @Test
    fun income_filterBySourceType_returnsCorrectSubset() {
        val incomes = listOf(
            Income("i1", 1000.0, "LKR", 1000.0, "SALARY", date = 0L),
            Income("i2", 500.0, "LKR", 500.0, "FREELANCE", date = 0L),
            Income("i3", 300.0, "LKR", 300.0, "SALARY", date = 0L)
        )
        val salaries = incomes.filter { it.sourceType == "SALARY" }
        assertEquals(2, salaries.size)
        assertEquals(1300.0, salaries.sumOf { it.amountLKR }, 0.0)
    }

    @Test
    fun income_notes_canBeStored() {
        val income = Income("i", 100.0, "LKR", 100.0, "SALARY", date = 0L, notes = "Bonus payment")
        assertEquals("Bonus payment", income.notes)
    }

    @Test
    fun income_projectRef_canBeStored() {
        val income = Income("i", 500.0, "USD", 150_000.0, "FREELANCE", date = 0L, projectRef = "Client ABC")
        assertEquals("Client ABC", income.projectRef)
    }
}
