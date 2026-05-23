package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal

interface BudgetRepository {
    suspend fun upsertGoal(goal: Goal)
    suspend fun getLatestGoal(): Goal?
    suspend fun updateGoalSavings(id: String, newSavings: Double)

    suspend fun upsertBudgetCategory(category: BudgetCategory)
    suspend fun getBudgetCategoriesForMonth(monthYear: String): List<BudgetCategory>
    suspend fun getActualSpentByCategory(monthYear: String): Map<String, Double>
}

