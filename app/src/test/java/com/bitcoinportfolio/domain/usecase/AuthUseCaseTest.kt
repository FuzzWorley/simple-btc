package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.data.secure.PinHasher
import com.bitcoinportfolio.data.secure.SecureStorage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthUseCaseTest {

    private val secureStorage: SecureStorage = mockk()
    private val pinHasher = PinHasher()
    private val useCase = AuthUseCase(secureStorage, pinHasher)

    @Test
    fun `authenticateWithPin returns Success for correct pin`() {
        val salt = pinHasher.generateSalt()
        every { secureStorage.getPinSalt() } returns salt
        every { secureStorage.getPinHash() } returns pinHasher.hash("1234", salt)

        assertEquals(AuthResult.Success, useCase.authenticateWithPin("1234"))
    }

    @Test
    fun `authenticateWithPin returns Failure for wrong pin`() {
        val salt = pinHasher.generateSalt()
        every { secureStorage.getPinSalt() } returns salt
        every { secureStorage.getPinHash() } returns pinHasher.hash("1234", salt)

        assertEquals(AuthResult.Failure, useCase.authenticateWithPin("9999"))
    }

    @Test
    fun `authenticateWithPin returns Failure when salt is missing`() {
        every { secureStorage.getPinSalt() } returns null

        assertEquals(AuthResult.Failure, useCase.authenticateWithPin("1234"))
    }

    @Test
    fun `authenticateWithPin returns Failure when hash is missing`() {
        every { secureStorage.getPinSalt() } returns pinHasher.generateSalt()
        every { secureStorage.getPinHash() } returns null

        assertEquals(AuthResult.Failure, useCase.authenticateWithPin("1234"))
    }

    @Test
    fun `generateAndStoreSpoofAmount returns value between 0_01 and 10_0`() {
        every { secureStorage.saveSpoofAmount(any()) } just runs

        repeat(50) {
            val spoof = useCase.generateAndStoreSpoofAmount()
            assertTrue(spoof >= 0.01 && spoof <= 10.0, "Spoof $spoof out of expected range")
        }
    }

    @Test
    fun `generateAndStoreSpoofAmount produces different values across calls`() {
        every { secureStorage.saveSpoofAmount(any()) } just runs

        val values = (1..20).map { useCase.generateAndStoreSpoofAmount() }.toSet()
        assertTrue(values.size > 1, "Expected variation in spoof amounts")
    }

    @Test
    fun `generateAndStoreSpoofAmount saves the returned value to storage`() {
        val savedSlot = slot<Double>()
        every { secureStorage.saveSpoofAmount(capture(savedSlot)) } just runs

        val returned = useCase.generateAndStoreSpoofAmount()

        assertEquals(returned, savedSlot.captured)
    }

    @Test
    fun `generateAndStoreSpoofAmount result has at most 8 decimal places`() {
        every { secureStorage.saveSpoofAmount(any()) } just runs

        repeat(20) {
            val spoof = useCase.generateAndStoreSpoofAmount()
            val decimalPlaces = spoof.toBigDecimal().stripTrailingZeros().scale()
            assertTrue(decimalPlaces <= 8, "Expected max 8 decimal places, got $decimalPlaces for $spoof")
        }
    }

    @Test
    fun `getRealAmount returns stored btc amount`() {
        every { secureStorage.getBtcAmount() } returns 0.58392710

        assertEquals(0.58392710, useCase.getRealAmount())
    }

    @Test
    fun `getRealAmount returns 0_0 when storage is empty`() {
        every { secureStorage.getBtcAmount() } returns null

        assertEquals(0.0, useCase.getRealAmount())
    }

    @Test
    fun `authenticateWithPin does not access storage when salt is missing`() {
        every { secureStorage.getPinSalt() } returns null

        useCase.authenticateWithPin("1234")

        verify(exactly = 0) { secureStorage.getPinHash() }
    }
}
