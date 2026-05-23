package com.example.financeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.financeapp.domain.model.Budget
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.Income

@Database(entities = [Income::class, Expense::class, Goal::class, Budget::class], version = 3, exportSchema = false)
abstract class IncomeDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun budgetDao(): BudgetDao
}

