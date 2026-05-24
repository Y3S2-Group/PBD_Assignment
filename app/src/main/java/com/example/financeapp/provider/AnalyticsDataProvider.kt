package com.example.financeapp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.example.financeapp.data.local.AnalyticsDatabaseLocator

class AnalyticsDataProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val context = context ?: return MatrixCursor(COLUMNS)
        val summary = when (uriMatcher.match(uri)) {
            SUMMARY_LATEST -> runCatching { AnalyticsDatabaseLocator.get(context).analyticsDao().getLatest() }.getOrNull()
            else -> null
        }

        val cursor = MatrixCursor(COLUMNS)
        summary?.let {
            cursor.addRow(
                arrayOf<Any?>(
                    it.monthKey,
                    it.totalIncomeLkr,
                    it.totalExpenseLkr,
                    it.netSavingsLkr,
                    it.savingsRate,
                    it.healthScore,
                    it.goalTitle,
                    it.goalProgress,
                    it.committedPercentage,
                    it.discretionaryPercentage,
                    it.topCategoryLabel,
                    it.topCategoryAmountLkr,
                    it.topIncomeSourceLabel,
                    it.topIncomeSourceAmountLkr,
                    it.primaryInsightTitle,
                    it.primaryInsightMessage,
                    it.updatedAt
                )
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            SUMMARY_LATEST -> "vnd.android.cursor.item/vnd.$AUTHORITY.analytics_summary"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        private const val AUTHORITY = "com.example.financeapp.analytics"
        private const val SUMMARY_LATEST = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "summary/latest", SUMMARY_LATEST)
        }

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/summary/latest")

        private val COLUMNS = arrayOf(
            "monthKey",
            "totalIncomeLkr",
            "totalExpenseLkr",
            "netSavingsLkr",
            "savingsRate",
            "healthScore",
            "goalTitle",
            "goalProgress",
            "committedPercentage",
            "discretionaryPercentage",
            "topCategoryLabel",
            "topCategoryAmountLkr",
            "topIncomeSourceLabel",
            "topIncomeSourceAmountLkr",
            "primaryInsightTitle",
            "primaryInsightMessage",
            "updatedAt"
        )
    }
}
