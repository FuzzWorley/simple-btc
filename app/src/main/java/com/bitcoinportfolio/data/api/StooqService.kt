package com.bitcoinportfolio.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface StooqService {

    @GET("q/d/l/")
    suspend fun getHistoricalData(
        @Query("s") symbol: String = "xauusd",
        @Query("i") interval: String = "d",
        @Query("d1") from: String,
        @Query("d2") to: String
    ): ResponseBody
}
