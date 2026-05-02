package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.data.secure.PinHasher
import com.bitcoinportfolio.data.secure.SecureStorage
import javax.inject.Inject

class SetupPortfolioUseCase @Inject constructor(
    private val secureStorage: SecureStorage,
    private val pinHasher: PinHasher,
) {
    fun execute(btcAmount: Double, pin: String) {
        val salt = pinHasher.generateSalt()
        val hash = pinHasher.hash(pin, salt)
        secureStorage.saveBtcAmount(btcAmount)
        secureStorage.savePinHash(hash, salt)
        secureStorage.markSetupComplete()
    }
}
