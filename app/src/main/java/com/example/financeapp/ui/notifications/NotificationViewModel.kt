package com.example.financeapp.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.repository.BudgetRepository
import com.example.financeapp.domain.repository.ExpenseRepository
import com.example.financeapp.domain.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val expenseRepo: ExpenseRepository,
    private val incomeRepo: IncomeRepository,
    private val budgetRepo: BudgetRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val items = mutableListOf<NotificationItem>()
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val yesterdayStart = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val currentMonth = YearMonth.now()
            val monthYear = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val monthEnd =
                currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            // ── 1. Budget alerts ──────────────────────────────────────────
            val budgetCategories = budgetRepo.getBudgetCategoriesForMonth(monthYear)
            val actualSpent = budgetRepo.getActualSpentByCategory(monthYear)

            budgetCategories.forEach { category ->
                val spent = actualSpent[category.categoryName] ?: 0.0
                val allocated = category.allocatedAmount
                if (allocated > 0) {
                    val pct = (spent / allocated * 100).toInt()
                    when {
                        pct >= 100 -> items += NotificationItem(
                            id = "budget_exceeded_${category.id}",
                            title = "Budget Exceeded",
                            description = "${category.categoryName}: LKR ${spent.toInt()} of LKR ${allocated.toInt()} budget spent",
                            type = NotificationType.Warning,
                            icon = NotificationIcon.Warning,
                            timestampLabel = "Now",
                            isRead = false,
                            section = NotificationSection.Today,
                            sortKey = now,
                        )

                        pct >= 80 -> items += NotificationItem(
                            id = "budget_alert_${category.id}",
                            title = "Budget Alert",
                            description = "${category.categoryName}: $pct% of LKR ${allocated.toInt()} budget used",
                            type = NotificationType.Warning,
                            icon = NotificationIcon.Warning,
                            timestampLabel = "Now",
                            isRead = false,
                            section = NotificationSection.Today,
                            sortKey = now - 1,
                        )
                    }
                }
            }

            // ── 2. Goal alerts ────────────────────────────────────────────
            val goal = budgetRepo.getLatestGoal()
            if (goal != null) {
                val pct =
                    if (goal.targetAmount > 0) (goal.currentSavings / goal.targetAmount * 100).toInt() else 0
                val daysRemaining = ((goal.deadlineTimestamp - now) / DAY_MILLIS).toInt()

                // Deadline approaching within 7 days
                if (daysRemaining in 1..7) {
                    items += NotificationItem(
                        id = "goal_deadline",
                        title = "Goal Deadline Soon",
                        description = "${goal.name}: $daysRemaining day(s) left — LKR ${goal.currentSavings.toInt()} of LKR ${goal.targetAmount.toInt()} saved",
                        type = NotificationType.Warning,
                        icon = NotificationIcon.Goal,
                        timestampLabel = "Now",
                        isRead = false,
                        section = NotificationSection.Today,
                        sortKey = now - 2,
                    )
                }

                // Milestone notifications (100 → 75 → 50 → 25)
                val milestone = listOf(100, 75, 50, 25).firstOrNull { pct >= it }
                if (milestone != null) {
                    items += NotificationItem(
                        id = "goal_milestone_$milestone",
                        title = if (milestone >= 100) "Goal Achieved!" else "Goal Milestone Reached",
                        description = "${goal.name}: $milestone% reached — LKR ${goal.currentSavings.toInt()} saved of LKR ${goal.targetAmount.toInt()}",
                        type = if (milestone >= 100) NotificationType.Success else NotificationType.Info,
                        icon = NotificationIcon.Goal,
                        timestampLabel = "Today",
                        isRead = milestone < 75,
                        section = NotificationSection.Today,
                        sortKey = now - 3,
                    )
                }
            }

            // ── 3. Recent income (today & yesterday) ──────────────────────
            val incomes = incomeRepo.getAllIncomes()
            incomes
                .filter { it.date >= yesterdayStart }
                .sortedByDescending { it.date }
                .take(3)
                .forEach { income ->
                    val label =
                        income.sourceLabel?.takeIf { it.isNotBlank() } ?: income.sourceType
                    items += NotificationItem(
                        id = "income_${income.id}",
                        title = "Income Received",
                        description = "$label: LKR ${income.amountLKR.toInt()}",
                        type = NotificationType.Success,
                        icon = NotificationIcon.Wallet,
                        timestampLabel = timeLabel(income.date, now),
                        isRead = income.date < todayStart,
                        section = sectionFor(income.date, todayStart, yesterdayStart),
                        sortKey = income.date,
                    )
                }

            // ── 4. Recent & large expenses ────────────────────────────────
            val incomeThisMonth = incomeRepo.sumAmountLkrBetween(monthStart, monthEnd)
            val largeThreshold = (incomeThisMonth * 0.15).coerceAtLeast(5_000.0)
            val allExpenses = expenseRepo.getAllExpenses()

            allExpenses
                .filter { it.timestamp >= yesterdayStart || it.amountLkr >= largeThreshold }
                .sortedByDescending { it.timestamp }
                .take(5)
                .forEach { expense ->
                    val isLarge = expense.amountLkr >= largeThreshold
                    val suffix = when {
                        expense.spendingType.equals("COMMITTED", ignoreCase = true) -> " (recurring)"
                        isLarge -> " ⚠"
                        else -> ""
                    }
                    items += NotificationItem(
                        id = "expense_${expense.id}",
                        title = if (isLarge) "Large Expense" else "Expense Logged",
                        description = "${expense.category}: LKR ${expense.amountLkr.toInt()}$suffix",
                        type = if (isLarge) NotificationType.Warning else NotificationType.Info,
                        icon = NotificationIcon.Receipt,
                        timestampLabel = timeLabel(expense.timestamp, now),
                        isRead = expense.timestamp < todayStart,
                        section = sectionFor(expense.timestamp, todayStart, yesterdayStart),
                        sortKey = expense.timestamp,
                    )
                }

            // ── 5. Low savings-rate alert ─────────────────────────────────
            val expenseThisMonth = expenseRepo.sumAmountLkrBetween(monthStart, monthEnd)
            if (incomeThisMonth > 0 && expenseThisMonth > incomeThisMonth * 0.9) {
                val ratio = (expenseThisMonth / incomeThisMonth * 100).toInt()
                items += NotificationItem(
                    id = "low_savings_alert",
                    title = "Low Savings Alert",
                    description = "You've spent $ratio% of this month's income. Review your discretionary spending.",
                    type = NotificationType.Warning,
                    icon = NotificationIcon.Trending,
                    timestampLabel = "Now",
                    isRead = false,
                    section = NotificationSection.Today,
                    sortKey = now - 4,
                )
            }

            // Sort: Today → Yesterday → Earlier, then newest-first within each section
            val sorted = items
                .distinctBy { it.id }
                .sortedWith(compareBy({ it.section.ordinal }, { -it.sortKey }))

            // ── AI insight ────────────────────────────────────────────────
            val (insightTitle, insightText) = buildInsight(goal, incomeThisMonth, expenseThisMonth)

            _state.value = NotificationUiState(
                notifications = sorted,
                isLoading = false,
                insightTitle = insightTitle,
                insightText = insightText,
            )
        }
    }

    fun markAllAsRead() {
        _state.update { current ->
            current.copy(notifications = current.notifications.map { it.copy(isRead = true) })
        }
    }

    fun markAsRead(id: String) {
        _state.update { current ->
            current.copy(
                notifications = current.notifications.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildInsight(
        goal: Goal?,
        incomeThisMonth: Double,
        expenseThisMonth: Double,
    ): Pair<String, String> {
        if (goal != null && goal.targetAmount > 0) {
            val pct = (goal.currentSavings / goal.targetAmount * 100).toInt()
            val daysLeft =
                ((goal.deadlineTimestamp - System.currentTimeMillis()) / DAY_MILLIS).toInt()
                    .coerceAtLeast(0)
            val status = when {
                pct >= 100 -> "You've hit your target — congratulations!"
                daysLeft <= 7 -> "Final stretch! Push through to reach your goal."
                pct >= 75 -> "Almost there — keep the momentum going."
                pct >= 50 -> "Halfway done — stay consistent with deposits."
                else -> "Every deposit counts — you're making progress!"
            }
            return "Goal Progress" to
                    "You're $pct% towards \"${goal.name}\" with LKR ${goal.currentSavings.toInt()} saved of LKR ${goal.targetAmount.toInt()}. $status"
        }

        if (incomeThisMonth > 0) {
            val savingsRate =
                ((incomeThisMonth - expenseThisMonth) / incomeThisMonth * 100).toInt()
                    .coerceAtLeast(0)
            val tip = when {
                savingsRate >= 20 -> "Excellent! You're saving well above the recommended 20%."
                savingsRate >= 10 -> "Good progress — push your savings rate above 20% to build wealth faster."
                savingsRate > 0 -> "Consider trimming discretionary spending to strengthen your savings cushion."
                else -> "Your expenses are exceeding income this month — review your budget urgently."
            }
            return "Monthly Savings Rate" to
                    "You're saving $savingsRate% of your income this month. $tip"
        }

        return "Get Started" to
                "Log your income and expenses to unlock personalised financial insights and smart alerts."
    }

    private fun sectionFor(
        timestamp: Long,
        todayStart: Long,
        yesterdayStart: Long,
    ): NotificationSection = when {
        timestamp >= todayStart -> NotificationSection.Today
        timestamp >= yesterdayStart -> NotificationSection.Yesterday
        else -> NotificationSection.Earlier
    }

    private fun timeLabel(timestamp: Long, now: Long): String {
        val diff = now - timestamp
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000}h ago"
            diff < 172_800_000L -> "Yesterday"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1_000
    }
}

// ── UI state & models ─────────────────────────────────────────────────────────

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = true,
    val insightTitle: String = "",
    val insightText: String = "",
)

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val type: NotificationType,
    val icon: NotificationIcon,
    val timestampLabel: String,
    val isRead: Boolean,
    val section: NotificationSection,
    val sortKey: Long = 0L,
)

enum class NotificationType { Warning, Success, Info }

enum class NotificationIcon { Warning, Wallet, Security, Receipt, Goal, Trending }

enum class NotificationSection { Today, Yesterday, Earlier }
