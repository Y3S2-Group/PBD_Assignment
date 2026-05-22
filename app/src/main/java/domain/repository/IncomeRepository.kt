package domain.repository

import data.model.Currency
import data.model.Income
import data.model.IncomeSource
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    suspend fun addIncome(income: Income): Result<String>
    fun getIncomes(userId: String): Flow<List<Income>>
    suspend fun getTotalIncomeByCurrency(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Map<Currency, Double>
    suspend fun getIncomeBySource(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Map<IncomeSource, Map<Currency, Double>>
    suspend fun getIncomes(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<Income>
}
