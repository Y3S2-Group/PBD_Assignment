package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeapp.domain.model.Goal

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal)

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Goal?

    @Query("UPDATE goals SET currentSavings = :newSavings WHERE id = :id")
    suspend fun updateCurrentSavings(id: String, newSavings: Double)

    @Query("SELECT * FROM goals ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): Goal?
}
