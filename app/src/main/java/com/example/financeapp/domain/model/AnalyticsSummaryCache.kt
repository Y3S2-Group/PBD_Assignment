package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Base64

@Entity(tableName = "analytics_summary_cache")
data class AnalyticsSummaryCache(
    @PrimaryKey val monthKey: String,
    val totalIncomeLkr: Double,
    val totalExpenseLkr: Double,
    val netSavingsLkr: Double,
    val savingsRate: Double,
    val healthScore: Int,
    val goalTitle: String,
    val goalCurrentAmountLkr: Double,
    val goalTargetAmountLkr: Double,
    val goalDaysLeft: Int,
    val goalProgress: Float,
    val committedAmountLkr: Double,
    val discretionaryAmountLkr: Double,
    val committedPercentage: Double,
    val discretionaryPercentage: Double,
    val topCategoryLabel: String?,
    val topCategoryAmountLkr: Double,
    val topCategoryPercentage: Double,
    val topIncomeSourceLabel: String?,
    val topIncomeSourceAmountLkr: Double,
    val topIncomeSourcePercentage: Double,
    val categoryBreakdownPayload: String,
    val incomeBreakdownPayload: String,
    val monthlyTrendPayload: String,
    val insightsPayload: String,
    val primaryInsightTitle: String?,
    val primaryInsightMessage: String?,
    val primaryInsightTone: String?,
    val updatedAt: Long
) {
    companion object {
        fun from(summary: DashboardAnalytics, monthKey: String, updatedAt: Long): AnalyticsSummaryCache {
            val topCategory = summary.categoryBreakdown.firstOrNull()
            val topIncomeSource = summary.incomeBreakdown.firstOrNull()
            val primaryInsight = summary.insights.firstOrNull()
            return AnalyticsSummaryCache(
                monthKey = monthKey,
                totalIncomeLkr = summary.totalIncomeLkr,
                totalExpenseLkr = summary.totalExpenseLkr,
                netSavingsLkr = summary.netSavingsLkr,
                savingsRate = summary.savingsRate,
                healthScore = summary.healthScore,
                goalTitle = summary.goal.title,
                goalCurrentAmountLkr = summary.goal.currentAmountLkr,
                goalTargetAmountLkr = summary.goal.targetAmountLkr,
                goalDaysLeft = summary.goal.daysLeft,
                goalProgress = summary.goal.progress,
                committedAmountLkr = summary.spendingBreakdown.committedAmountLkr,
                discretionaryAmountLkr = summary.spendingBreakdown.discretionaryAmountLkr,
                committedPercentage = summary.spendingBreakdown.committedPercentage,
                discretionaryPercentage = summary.spendingBreakdown.discretionaryPercentage,
                topCategoryLabel = topCategory?.label,
                topCategoryAmountLkr = topCategory?.amountLkr ?: 0.0,
                topCategoryPercentage = topCategory?.percentage ?: 0.0,
                topIncomeSourceLabel = topIncomeSource?.label,
                topIncomeSourceAmountLkr = topIncomeSource?.amountLkr ?: 0.0,
                topIncomeSourcePercentage = topIncomeSource?.percentage ?: 0.0,
                categoryBreakdownPayload = encodeBreakdown(summary.categoryBreakdown),
                incomeBreakdownPayload = encodeBreakdown(summary.incomeBreakdown),
                monthlyTrendPayload = encodeTrend(summary.monthlyTrend),
                insightsPayload = encodeInsights(summary.insights),
                primaryInsightTitle = primaryInsight?.title,
                primaryInsightMessage = primaryInsight?.message,
                primaryInsightTone = primaryInsight?.tone?.name,
                updatedAt = updatedAt
            )
        }

        fun decodeBreakdown(payload: String): List<BreakdownSlice> {
            if (payload.isBlank()) return emptyList()
            return payload.lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size != 3) return@mapNotNull null
                    BreakdownSlice(
                        label = decodeString(parts[0]),
                        amountLkr = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                        percentage = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                    )
                }
                .toList()
        }

        fun decodeTrend(payload: String): List<TrendPoint> {
            if (payload.isBlank()) return emptyList()
            return payload.lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size != 4) return@mapNotNull null
                    TrendPoint(
                        label = decodeString(parts[0]),
                        incomeLkr = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                        expenseLkr = parts[2].toDoubleOrNull() ?: return@mapNotNull null,
                        savingsLkr = parts[3].toDoubleOrNull() ?: return@mapNotNull null
                    )
                }
                .toList()
        }

        fun decodeInsights(payload: String): List<DashboardInsight> {
            if (payload.isBlank()) return emptyList()
            return payload.lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size != 3) return@mapNotNull null
                    DashboardInsight(
                        title = decodeString(parts[0]),
                        message = decodeString(parts[1]),
                        tone = runCatching { enumValueOf<InsightTone>(parts[2]) }
                            .getOrDefault(InsightTone.Neutral)
                    )
                }
                .toList()
        }

        private fun encodeBreakdown(items: List<BreakdownSlice>): String =
            items.joinToString(separator = "\n") {
                "${encodeString(it.label)}|${it.amountLkr}|${it.percentage}"
            }

        private fun encodeTrend(items: List<TrendPoint>): String =
            items.joinToString(separator = "\n") {
                "${encodeString(it.label)}|${it.incomeLkr}|${it.expenseLkr}|${it.savingsLkr}"
            }

        private fun encodeInsights(items: List<DashboardInsight>): String =
            items.joinToString(separator = "\n") {
                "${encodeString(it.title)}|${encodeString(it.message)}|${it.tone.name}"
            }

        private fun encodeString(value: String): String =
            Base64.getUrlEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

        private fun decodeString(value: String): String =
            String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    }
}
