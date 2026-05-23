package com.example.financeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.domain.model.Income

@Database(entities = [Income::class, Expense::class], version = 2, exportSchema = false)
abstract class IncomeDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
}

