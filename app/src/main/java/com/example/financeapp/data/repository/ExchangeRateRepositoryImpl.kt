package com.example.financeapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.financeapp.data.remote.network.ExchangeRateApi
import com.example.financeapp.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepositoryImpl @Inject constructor(
    private val api: ExchangeRateApi,
    private val dataStore: DataStore<Preferences>
) : ExchangeRateRepository {

    override suspend fun getRate(base: String, target: String): Double {
        val baseLower = base.lowercase()
        val targetLower = target.lowercase()

        return try {
            val response = api.getExchangeRates(baseLower)
            // Response structure: { "date": "...", "base": { "target": rate } }
            val rates = response[baseLower] as? Map<String, Any>
            val rate = (rates?.get(targetLower) as? Double)
                ?: throw Exception("Target currency $target not found in API response")

            cacheRate(baseLower, targetLower, rate)
            rate
        } catch (e: Exception) {
            getCachedRate(baseLower, targetLower)
                ?: throw Exception("Cannot fetch exchange rate. Please check internet connection.")
        }
    }

    private suspend fun cacheRate(base: String, target: String, rate: Double) {
        val rateKey = doublePreferencesKey("rate_${base}_$target")
        val timestampKey = longPreferencesKey("timestamp_${base}_$target")
        dataStore.edit { prefs ->
            prefs[rateKey] = rate
            prefs[timestampKey] = System.currentTimeMillis()
        }
    }

    private suspend fun getCachedRate(base: String, target: String): Double? {
        val rateKey = doublePreferencesKey("rate_${base}_$target")
        val prefs = dataStore.data.first()
        return prefs[rateKey]
    }
}
