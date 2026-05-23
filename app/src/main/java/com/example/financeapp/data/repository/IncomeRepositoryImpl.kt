package com.example.financeapp.data.repository

import com.example.financeapp.data.local.IncomeDao
import com.example.financeapp.domain.model.Income
import com.example.financeapp.domain.repository.IncomeRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IncomeRepositoryImpl @Inject constructor(
    private val incomeDao: IncomeDao
) : IncomeRepository {
    override suspend fun insertIncome(income: Income) {
        withContext(Dispatchers.IO) {
            incomeDao.insert(income)
        }
    }

    override suspend fun updateIncome(income: Income) {
        withContext(Dispatchers.IO) {
            incomeDao.update(income)
        }
    }

    override suspend fun deleteIncome(id: String) {
        withContext(Dispatchers.IO) {
            incomeDao.deleteById(id)
        }
    }

    override suspend fun getAllIncomes(): List<Income> = withContext(Dispatchers.IO) {
        incomeDao.getAll()
    }

    override suspend fun getBySourceType(sourceType: String): List<Income> = withContext(Dispatchers.IO) {
        incomeDao.getBySourceType(sourceType)
    }

    override suspend fun sumAmountLkrBetween(startInclusive: Long, endInclusive: Long): Double =
        withContext(Dispatchers.IO) {
            incomeDao.sumAmountLkrBetween(startInclusive, endInclusive) ?: 0.0
        }
}
