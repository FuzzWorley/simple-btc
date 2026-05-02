package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.data.secure.PinHasher
import com.bitcoinportfolio.data.secure.SecureStorage
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupPortfolioUseCaseTest {

    private val secureStorage: SecureStorage = mockk(relaxed = true)
    private val pinHasher = PinHasher()
    private val useCase = SetupPortfolioUseCase(secureStorage, pinHasher)

    @Test
    fun `execute saves btc amount`() {
        useCase.execute(1.5, "1234")

        verifyOrder { secureStorage.saveBtcAmount(1.5) }
    }

    @Test
    fun `execute saves a pin hash that verifies correctly`() {
        val hashSlot = slot<String>()
        val saltSlot = slot<String>()
        // relaxed mock: capture the args without explicit every {} stub
        io.mockk.every { secureStorage.savePinHash(capture(hashSlot), capture(saltSlot)) } returns Unit

        useCase.execute(1.5, "1234")

        assertTrue(
            pinHasher.verify("1234", saltSlot.captured, hashSlot.captured),
            "Stored hash should verify against the original PIN"
        )
    }

    @Test
    fun `execute marks setup complete`() {
        useCase.execute(1.5, "1234")

        verifyOrder { secureStorage.markSetupComplete() }
    }

    @Test
    fun `execute saves amount and pin before marking setup complete`() {
        useCase.execute(1.5, "1234")

        verifyOrder {
            secureStorage.saveBtcAmount(1.5)
            secureStorage.savePinHash(any(), any())
            secureStorage.markSetupComplete()
        }
    }

    @Test
    fun `execute with different pins produces different hashes`() {
        val hashSlot1 = slot<String>()
        val hashSlot2 = slot<String>()

        io.mockk.every { secureStorage.savePinHash(capture(hashSlot1), any()) } returns Unit
        useCase.execute(1.0, "1111")

        io.mockk.every { secureStorage.savePinHash(capture(hashSlot2), any()) } returns Unit
        useCase.execute(1.0, "2222")

        assertTrue(hashSlot1.captured != hashSlot2.captured)
    }
}
