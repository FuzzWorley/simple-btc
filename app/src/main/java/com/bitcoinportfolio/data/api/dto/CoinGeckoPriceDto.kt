package com.bitcoinportfolio.data.api.dto

import com.google.gson.annotations.SerializedName

data class CoinGeckoPriceDto(
    @SerializedName("bitcoin") val bitcoin: BitcoinPriceData
) {
    data class BitcoinPriceData(
        @SerializedName("usd") val usd: Double
    )
}
