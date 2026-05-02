package com.bitcoinportfolio.data.repository

import com.bitcoinportfolio.data.secure.SecureStorage
import com.bitcoinportfolio.domain.repository.PortfolioRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage,
) : PortfolioRepository {

    override fun getBtcAmount(): Double = secureStorage.getBtcAmount() ?: 0.0

    override fun updateBtcAmount(amount: Double) = secureStorage.saveBtcAmount(amount)
}
