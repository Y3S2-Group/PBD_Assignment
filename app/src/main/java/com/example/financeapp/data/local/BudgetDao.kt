package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeapp.domain.model.Budget

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear LIMIT 1")
    suspend fun getByMonthYear(monthYear: String): Budget?
}

