package com.example.financeapp.data.remote.network

import com.google.gson.annotations.SerializedName

data class ExchangeRateDto(
    @SerializedName("date")
    val date: String,
    val rates: Map<String, Double>
)

/**
 * Since the API response has a dynamic key (e.g., "usd", "eth"),
 * we'll use this class to parse the full response where the dynamic key
 * maps to the actual rates object.
 */
data class DynamicExchangeRateResponse(
    val date: String,
    val baseCurrency: String,
    val rates: Map<String, Double>
)
