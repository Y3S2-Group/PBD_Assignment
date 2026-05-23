package com.example.financeapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.financeapp.domain.model.Expense
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExpenseDaoTest {
    private lateinit var database: IncomeDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, IncomeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.expenseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertExpenseAndRetrieve() = kotlinx.coroutines.runBlocking {
        val expense = Expense(
            id = "exp_1",
            amountLkr = 1250.0,
            category = "Food",
            spendingType = "DISCRETIONARY",
            paymentMethod = "Card",
            timestamp = System.currentTimeMillis()
        )

        dao.insert(expense)
        val retrieved = dao.getById("exp_1")

        assertNotNull(retrieved)
        assertEquals(expense, retrieved)
    }

    @Test
    fun filterBySpendingType() = kotlinx.coroutines.runBlocking {
        val expenses = listOf(
            Expense("exp_2", 300.0, "Transport", "COMMITTED", "Cash", System.currentTimeMillis()),
            Expense("exp_3", 450.0, "Food", "DISCRETIONARY", "Card", System.currentTimeMillis()),
            Expense("exp_4", 600.0, "Subs", "COMMITTED", "Card", System.currentTimeMillis())
        )

        expenses.forEach { dao.insert(it) }

        val committed = dao.getBySpendingType("COMMITTED")
        assertEquals(2, committed.size)
        assertEquals(true, committed.all { it.spendingType == "COMMITTED" })
    }

    @Test
    fun sumForCurrentMonth() = kotlinx.coroutines.runBlocking {
        val zone = ZoneId.of("UTC")
        val monthStart = LocalDate.of(2026, 5, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = LocalDate.of(2026, 5, 31).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val mayExpenses = listOf(
            Expense("exp_5", 1200.0, "Food", "DISCRETIONARY", "Card", monthStart + 10_000),
            Expense("exp_6", 800.0, "Transport", "COMMITTED", "Cash", monthStart + 20_000)
        )
        val aprilExpense = Expense("exp_7", 900.0, "Subs", "COMMITTED", "Card", monthStart - 86_400_000)

        mayExpenses.forEach { dao.insert(it) }
        dao.insert(aprilExpense)

        val sum = dao.sumAmountLkrBetween(monthStart, monthEnd) ?: 0.0
        assertEquals(2000.0, sum, 0.01)
    }
}

