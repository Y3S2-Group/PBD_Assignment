package com.example.financeapp.domain.model

data class DashboardAnalytics(
    val totalIncomeLkr: Double,
    val totalExpenseLkr: Double,
    val netSavingsLkr: Double,
    val savingsRate: Double,
    val healthScore: Int,
    val goal: GoalSnapshot,
    val categoryBreakdown: List<BreakdownSlice>,
    val incomeBreakdown: List<BreakdownSlice>,
    val spendingBreakdown: SpendingBreakdown,
    val monthlyTrend: List<TrendPoint>,
    val insights: List<DashboardInsight>
) {
    companion object {
        fun empty() = DashboardAnalytics(
            totalIncomeLkr = 0.0,
            totalExpenseLkr = 0.0,
            netSavingsLkr = 0.0,
            savingsRate = 0.0,
            healthScore = 0,
            goal = GoalSnapshot(
                title = "Set your monthly goal",
                currentAmountLkr = 0.0,
                targetAmountLkr = 150_000.0,
                daysLeft = 90,
                progress = 0f
            ),
            categoryBreakdown = emptyList(),
            incomeBreakdown = emptyList(),
            spendingBreakdown = SpendingBreakdown(0.0, 0.0, 0.0, 0.0),
            monthlyTrend = emptyList(),
            insights = emptyList()
        )
    }
}

data class GoalSnapshot(
    val title: String,
    val currentAmountLkr: Double,
    val targetAmountLkr: Double,
    val daysLeft: Int,
    val progress: Float
)

data class BreakdownSlice(
    val label: String,
    val amountLkr: Double,
    val percentage: Double
)

data class SpendingBreakdown(
    val committedAmountLkr: Double,
    val discretionaryAmountLkr: Double,
    val committedPercentage: Double,
    val discretionaryPercentage: Double
)

data class TrendPoint(
    val label: String,
    val incomeLkr: Double,
    val expenseLkr: Double,
    val savingsLkr: Double
)

data class DashboardInsight(
    val title: String,
    val message: String,
    val tone: InsightTone
)

enum class InsightTone {
    Positive,
    Warning,
    Neutral
}
