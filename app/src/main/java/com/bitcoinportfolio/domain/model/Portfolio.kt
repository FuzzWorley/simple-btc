package com.bitcoinportfolio.domain.model

data class Portfolio(
    val btcAmount: Double,
    val currentPrice: BitcoinPrice,
    val isSpoof: Boolean
) {
    val valueUsd: Double get() = btcAmount * currentPrice.usd
    val valueXau: Double get() = btcAmount * currentPrice.btcInXau
}
