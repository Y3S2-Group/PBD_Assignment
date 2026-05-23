package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.SavingsDeposit

interface BudgetRepository {
    suspend fun upsertGoal(goal: Goal)
    suspend fun getLatestGoal(): Goal?
    suspend fun updateGoalSavings(id: String, newSavings: Double)

    suspend fun upsertBudgetCategory(category: BudgetCategory)
    suspend fun getBudgetCategoriesForMonth(monthYear: String): List<BudgetCategory>
    suspend fun getActualSpentByCategory(monthYear: String): Map<String, Double>

    suspend fun insertDeposit(deposit: SavingsDeposit)
    suspend fun getDepositsForGoal(goalId: String): List<SavingsDeposit>

    suspend fun getMonthlyIncomeAverage(monthsBack: Int): Double
}
