package com.example.financeapp.domain.repository

interface ExchangeRateRepository {
    suspend fun getRate(base: String, target: String = "lkr"): Double
}
