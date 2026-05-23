package com.example.financeapp.data.repository

import com.example.financeapp.data.local.BudgetDao
import com.example.financeapp.data.local.ExpenseDao
import com.example.financeapp.data.local.GoalDao
import com.example.financeapp.data.local.IncomeDao
import com.example.financeapp.data.local.SavingsDepositDao
import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.SavingsDeposit
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
    private val expenseDao: ExpenseDao,
    private val savingsDepositDao: SavingsDepositDao,
    private val incomeDao: IncomeDao
) : BudgetRepository {

    override suspend fun upsertGoal(goal: Goal) {
        withContext(Dispatchers.IO) { goalDao.insert(goal) }
    }

    override suspend fun getLatestGoal(): Goal? = withContext(Dispatchers.IO) {
        goalDao.getLatest()
    }

    override suspend fun updateGoalSavings(id: String, newSavings: Double) {
        withContext(Dispatchers.IO) { goalDao.updateCurrentSavings(id, newSavings) }
    }

    override suspend fun upsertBudgetCategory(category: BudgetCategory) {
        withContext(Dispatchers.IO) { budgetDao.insert(category) }
    }

    override suspend fun getBudgetCategoriesForMonth(monthYear: String): List<BudgetCategory> =
        withContext(Dispatchers.IO) { budgetDao.getByMonthYear(monthYear) }

    override suspend fun getActualSpentByCategory(monthYear: String): Map<String, Double> =
        withContext(Dispatchers.IO) {
            val (start, end) = monthRange(monthYear)
            expenseDao.sumAmountLkrByCategoryBetween(start, end)
                .associate { it.category to (it.total ?: 0.0) }
        }

    override suspend fun insertDeposit(deposit: SavingsDeposit) {
        withContext(Dispatchers.IO) { savingsDepositDao.insert(deposit) }
    }

    override suspend fun getDepositsForGoal(goalId: String): List<SavingsDeposit> =
        withContext(Dispatchers.IO) { savingsDepositDao.getAllForGoal(goalId) }

    override suspend fun getMonthlyIncomeAverage(monthsBack: Int): Double =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val now = YearMonth.now()
            val totals = (1..monthsBack).map { offset ->
                val month = now.minusMonths(offset.toLong())
                val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                incomeDao.sumAmountLkrBetween(start, end) ?: 0.0
            }
            if (totals.all { it == 0.0 }) 0.0 else totals.average()
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
