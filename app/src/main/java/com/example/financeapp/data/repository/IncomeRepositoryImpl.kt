package com.example.financeapp.data.repository

import com.example.financeapp.data.local.IncomeDao
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.repository.IncomeRepository
import javax.inject.Inject

class IncomeRepositoryImpl @Inject constructor(
    private val incomeDao: IncomeDao
) : IncomeRepository {
    override suspend fun insertIncome(income: Income) {
        incomeDao.insert(income)
    }

    override suspend fun getAllIncomes(): List<Income> = incomeDao.getAll()

    override suspend fun getBySourceType(sourceType: String): List<Income> =
        incomeDao.getBySourceType(sourceType)

    override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
        incomeDao.sumAmountLkrBetween(startInclusive, endInclusive) ?: 0.0
}

