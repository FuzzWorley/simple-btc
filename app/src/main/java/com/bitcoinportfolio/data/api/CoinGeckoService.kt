package com.bitcoinportfolio.data.api

import com.bitcoinportfolio.data.api.dto.CoinGeckoHistoryDto
import com.bitcoinportfolio.data.api.dto.CoinGeckoPriceDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoService {

    @GET("api/v3/simple/price")
    suspend fun getCurrentPrice(
        @Query("ids") ids: String = "bitcoin",
        @Query("vs_currencies") currencies: String = "usd"
    ): CoinGeckoPriceDto

    @GET("api/v3/coins/bitcoin/market_chart")
    suspend fun getPriceHistory(
        @Query("vs_currency") currency: String = "usd",
        @Query("days") days: Int,
        @Query("precision") precision: Int = 2
    ): CoinGeckoHistoryDto
}
