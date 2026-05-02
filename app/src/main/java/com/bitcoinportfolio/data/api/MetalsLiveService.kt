package com.bitcoinportfolio.data.api

import com.bitcoinportfolio.data.api.dto.MetalsLiveDto
import retrofit2.http.GET

interface MetalsLiveService {

    @GET("api/spot")
    suspend fun getSpotPrices(): List<MetalsLiveDto>
}
