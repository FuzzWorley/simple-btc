package com.bitcoinportfolio.data.api.dto

import com.google.gson.annotations.SerializedName

data class CoinGeckoHistoryDto(
    // Each inner list is [timestamp_ms, price_usd]
    @SerializedName("prices") val prices: List<List<Double>>
)
