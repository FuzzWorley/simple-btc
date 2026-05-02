package com.bitcoinportfolio.domain.model

data class BitcoinPrice(
    val usd: Double,
    val xauPerOz: Double,
    val btcInXau: Double,
    val fetchedAtMs: Long,
    val isStale: Boolean
)
