package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.data.secure.SecureStorage
import javax.inject.Inject

class UpdatePortfolioAmountUseCase @Inject constructor(
    private val secureStorage: SecureStorage,
) {
    sealed class Result {
        object Success : Result()
        object InvalidAmount : Result()
    }

    fun execute(btcAmount: Double): Result {
        if (btcAmount <= 0) return Result.InvalidAmount
        val scale = btcAmount.toBigDecimal().stripTrailingZeros().scale()
        if (scale > 8) return Result.InvalidAmount
        secureStorage.saveBtcAmount(btcAmount)
        return Result.Success
    }
}
