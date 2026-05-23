package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.Budget
import com.example.financeapp.domain.model.Goal

interface BudgetGoalRepository {
    suspend fun insertGoal(goal: Goal)
    suspend fun getGoalById(id: String): Goal?
    suspend fun updateGoalSavings(id: String, newSavings: Double)

    suspend fun insertBudget(budget: Budget)
    suspend fun getBudgetByMonth(monthYear: String): Budget?
}
