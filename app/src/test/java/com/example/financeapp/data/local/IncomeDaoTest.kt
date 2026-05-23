package com.example.financeapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.financeapp.domain.model.Income
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
class IncomeDaoTest {
    private lateinit var database: TestIncomeDatabase
    private lateinit var dao: IncomeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TestIncomeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.incomeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertIncomeAndRetrieveById() = kotlinx.coroutines.runBlocking {
        val income = Income(
            id = "inc_salary_01",
            amount = 4200.0,
            currency = "USD",
            amountLKR = 1260000.0,
            sourceType = "SALARY",
            sourceLabel = null,
            date = System.currentTimeMillis()
        )

        dao.insert(income)
        val retrieved = dao.getById("inc_salary_01")

        assertNotNull(retrieved)
        assertEquals(income, retrieved)
    }

    @Test
    fun filterIncomeBySourceType() = kotlinx.coroutines.runBlocking {
        val entries = listOf(
            Income("inc_freelance_01", 500.0, "USD", 150000.0, "FREELANCE", null, System.currentTimeMillis()),
            Income("inc_adsense_01", 150.0, "USD", 45000.0, "ADSENSE", null, System.currentTimeMillis()),
            Income("inc_freelance_02", 300.0, "USD", 90000.0, "FREELANCE", null, System.currentTimeMillis())
        )

        entries.forEach { dao.insert(it) }

        val freelanceEntries = dao.getBySourceType("FREELANCE")
        assertEquals(2, freelanceEntries.size)
        assertEquals(true, freelanceEntries.all { it.sourceType == "FREELANCE" })
    }

    @Test
    fun sumIncomeForSpecificMonth() = kotlinx.coroutines.runBlocking {
        val zone = ZoneId.of("UTC")
        val monthStart = LocalDate.of(2026, 5, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = LocalDate.of(2026, 5, 31).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val mayIncome = listOf(
            Income("inc_may_01", 200.0, "USD", 60000.0, "FREELANCE", null, monthStart + 10_000),
            Income("inc_may_02", 120.0, "USD", 36000.0, "ADSENSE", null, monthStart + 20_000)
        )
        val aprilIncome = Income("inc_apr_01", 100.0, "USD", 30000.0, "ADSENSE", null, monthStart - 86_400_000)

        mayIncome.forEach { dao.insert(it) }
        dao.insert(aprilIncome)

        val sum = dao.sumAmountLkrBetween(monthStart, monthEnd) ?: 0.0
        assertEquals(96000.0, sum, 0.01)
    }
}
