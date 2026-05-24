package com.example.financeapp.data.remote.network

import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {
    @GET("currencies/{base}.json")
    suspend fun getExchangeRates(
        @Path("base") base: String
    ): Map<String, Any>
}
