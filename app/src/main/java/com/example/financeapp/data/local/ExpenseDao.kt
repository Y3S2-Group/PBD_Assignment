package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeapp.domain.model.Expense
import com.example.financeapp.data.local.ExpenseCategoryTotal

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Expense?

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<Expense>

    @Query("SELECT * FROM expenses WHERE spendingType = :spendingType")
    suspend fun getBySpendingType(spendingType: String): List<Expense>

    @Query("SELECT SUM(amountLkr) FROM expenses WHERE spendingType = :spendingType")
    suspend fun sumAmountLkrBySpendingType(spendingType: String): Double?

    @Query("SELECT SUM(amountLkr) FROM expenses WHERE timestamp BETWEEN :startInclusive AND :endInclusive")
    suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double?

    @Query("SELECT category AS category, SUM(amountLkr) AS total FROM expenses WHERE timestamp BETWEEN :startInclusive AND :endInclusive GROUP BY category")
    suspend fun sumAmountLkrByCategoryBetween(startInclusive: Long, endInclusive: Long): List<ExpenseCategoryTotal>

    @Query("SELECT SUM(amountLkr) FROM expenses WHERE spendingType = :spendingType AND timestamp BETWEEN :startInclusive AND :endInclusive")
    suspend fun sumAmountLkrBySpendingTypeBetween(spendingType: String, startInclusive: Long, endInclusive: Long): Double?
}
