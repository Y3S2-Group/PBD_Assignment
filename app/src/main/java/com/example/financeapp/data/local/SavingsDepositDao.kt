package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeapp.domain.model.SavingsDeposit

@Dao
interface SavingsDepositDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deposit: SavingsDeposit)

    @Query("SELECT * FROM savings_deposits WHERE goalId = :goalId ORDER BY timestamp ASC")
    suspend fun getAllForGoal(goalId: String): List<SavingsDeposit>

    @Query(
        "SELECT SUM(amount) FROM savings_deposits " +
            "WHERE goalId = :goalId AND timestamp BETWEEN :start AND :end"
    )
    suspend fun sumAmountBetween(goalId: String, start: Long, end: Long): Double?
}
