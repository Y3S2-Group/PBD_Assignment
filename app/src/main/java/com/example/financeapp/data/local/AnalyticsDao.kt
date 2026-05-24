package com.example.financeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeapp.domain.model.AnalyticsSummaryCache

@Dao
interface AnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: AnalyticsSummaryCache)

    @Query("SELECT * FROM analytics_summary_cache ORDER BY updatedAt DESC LIMIT 1")
    fun getLatest(): AnalyticsSummaryCache?

    @Query("SELECT * FROM analytics_summary_cache WHERE monthKey = :monthKey LIMIT 1")
    fun getByMonthKey(monthKey: String): AnalyticsSummaryCache?
}
