package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.DashboardAnalytics

interface AnalyticsRepository {
    suspend fun buildDashboardSummary(nowMillis: Long = System.currentTimeMillis()): DashboardAnalytics
    suspend fun refreshDashboardSummary(nowMillis: Long = System.currentTimeMillis()): DashboardAnalytics
    suspend fun getCachedDashboardSummary(): DashboardAnalytics?
}
