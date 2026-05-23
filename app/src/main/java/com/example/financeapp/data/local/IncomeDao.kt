package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financeapp.domain.model.Income

@Dao
interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(income: Income)

    @Update
    suspend fun update(income: Income)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM incomes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Income?

    @Query("SELECT * FROM incomes")
    suspend fun getAll(): List<Income>

    @Query("SELECT * FROM incomes WHERE sourceType = :sourceType")
    suspend fun getBySourceType(sourceType: String): List<Income>

    @Query("SELECT SUM(amountLKR) FROM incomes WHERE date BETWEEN :startInclusive AND :endInclusive")
    suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double?
}
