package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeapp.domain.model.BudgetCategory

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: BudgetCategory)

    @Query("SELECT * FROM budget_categories WHERE monthYear = :monthYear")
    suspend fun getByMonthYear(monthYear: String): List<BudgetCategory>
}
