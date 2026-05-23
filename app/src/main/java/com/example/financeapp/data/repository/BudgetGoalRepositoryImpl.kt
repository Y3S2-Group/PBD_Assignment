package com.example.financeapp.data.repository

import com.example.financeapp.data.local.BudgetDao
import com.example.financeapp.data.local.GoalDao
import com.example.financeapp.domain.model.Budget
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.repository.BudgetGoalRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BudgetGoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val budgetDao: BudgetDao
) : BudgetGoalRepository {
    override suspend fun insertGoal(goal: Goal) {
        withContext(Dispatchers.IO) {
            goalDao.insert(goal)
        }
    }

    override suspend fun getGoalById(id: String): Goal? = withContext(Dispatchers.IO) {
        goalDao.getById(id)
    }

    override suspend fun updateGoalSavings(id: String, newSavings: Double) {
        withContext(Dispatchers.IO) {
            goalDao.updateCurrentSavings(id, newSavings)
        }
    }

    override suspend fun insertBudget(budget: Budget) {
        withContext(Dispatchers.IO) {
            budgetDao.insert(budget)
        }
    }

    override suspend fun getBudgetByMonth(monthYear: String): Budget? = withContext(Dispatchers.IO) {
        budgetDao.getByMonthYear(monthYear)
    }
}

