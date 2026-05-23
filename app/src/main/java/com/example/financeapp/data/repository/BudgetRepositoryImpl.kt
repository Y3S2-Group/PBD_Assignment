package com.example.financeapp.data.repository

import com.example.financeapp.data.local.BudgetDao
import com.example.financeapp.data.local.ExpenseDao
import com.example.financeapp.data.local.GoalDao
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.repository.BudgetRepository
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao
) : BudgetRepository {
    override suspend fun upsertGoal(goal: Goal) {
        withContext(Dispatchers.IO) {
            goalDao.insert(goal)
        }
    }

    override suspend fun getLatestGoal(): Goal? = withContext(Dispatchers.IO) {
        goalDao.getLatest()
    }

    override suspend fun updateGoalSavings(id: String, newSavings: Double) {
        withContext(Dispatchers.IO) {
            goalDao.updateCurrentSavings(id, newSavings)
        }
    }

    override suspend fun upsertBudgetCategory(category: BudgetCategory) {
        withContext(Dispatchers.IO) {
            budgetDao.insert(category)
        }
    }

    override suspend fun getBudgetCategoriesForMonth(monthYear: String): List<BudgetCategory> =
        withContext(Dispatchers.IO) {
            budgetDao.getByMonthYear(monthYear)
        }

    override suspend fun getActualSpentByCategory(monthYear: String): Map<String, Double> =
        withContext(Dispatchers.IO) {
            val (start, end) = monthRange(monthYear)
            expenseDao.sumAmountLkrByCategoryBetween(start, end)
                .associate { it.category to (it.total ?: 0.0) }
        }

    private fun monthRange(monthYear: String): Pair<Long, Long> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val zone = ZoneId.systemDefault()
        val parsed = runCatching { YearMonth.parse(monthYear, formatter) }
            .getOrElse { YearMonth.now() }
        val start = parsed.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = parsed.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to (endExclusive - 1)
    }
}

