package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeapp.domain.model.RecurringIncome

@Dao
interface RecurringIncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringIncome)

    @Query("SELECT * FROM recurring_incomes WHERE isActive = 1 ORDER BY dayOfMonth ASC")
    suspend fun getAllActive(): List<RecurringIncome>

    @Query("SELECT * FROM recurring_incomes ORDER BY dayOfMonth ASC")
    suspend fun getAll(): List<RecurringIncome>

    @Query("UPDATE recurring_incomes SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Query("DELETE FROM recurring_incomes WHERE id = :id")
    suspend fun deleteById(id: String)
}
