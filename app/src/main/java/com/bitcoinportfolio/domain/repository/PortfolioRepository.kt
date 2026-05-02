package com.bitcoinportfolio.domain.repository

interface PortfolioRepository {
    fun getBtcAmount(): Double
    fun updateBtcAmount(amount: Double)
}
