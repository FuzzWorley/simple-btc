package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.data.secure.PinHasher
import com.bitcoinportfolio.data.secure.SecureStorage
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.random.Random

class AuthUseCase @Inject constructor(
    private val secureStorage: SecureStorage,
    private val pinHasher: PinHasher,
) {
    fun authenticateWithPin(pin: String): AuthResult {
        val salt = secureStorage.getPinSalt() ?: return AuthResult.Failure
        val storedHash = secureStorage.getPinHash() ?: return AuthResult.Failure
        return if (pinHasher.verify(pin, salt, storedHash)) AuthResult.Success
        else AuthResult.Failure
    }

    fun generateAndStoreSpoofAmount(): Double {
        val spoof = Random.nextDouble(0.01, 10.0)
            .toBigDecimal().setScale(8, RoundingMode.HALF_UP).toDouble()
        secureStorage.saveSpoofAmount(spoof)
        return spoof
    }

    fun getRealAmount(): Double = secureStorage.getBtcAmount() ?: 0.0
}

sealed class AuthResult {
    object Success : AuthResult()
    object Failure : AuthResult()
}
