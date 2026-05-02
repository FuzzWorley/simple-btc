package com.bitcoinportfolio.ui.auth

import android.content.Context
import com.bitcoinportfolio.data.secure.PinHasher
import com.bitcoinportfolio.data.secure.SecureStorage
import com.bitcoinportfolio.domain.usecase.AuthUseCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // Use a relaxed mock so BiometricManager.from(context) is caught by runCatching in init
    private val context: Context = mockk(relaxed = true)
    private val secureStorage: SecureStorage = mockk()
    private val pinHasher = PinHasher()
    private val authUseCase = AuthUseCase(secureStorage, pinHasher)

    private val correctPin = "1234"
    private lateinit var salt: String
    private lateinit var viewModel: AuthViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        salt = pinHasher.generateSalt()
        every { secureStorage.getPinSalt() } returns salt
        every { secureStorage.getPinHash() } returns pinHasher.hash(correctPin, salt)
        every { secureStorage.saveSpoofAmount(any()) } just runs
        viewModel = AuthViewModel(authUseCase, context)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `digit entry increments entered digits count`() {
        viewModel.onDigitEntered('1')
        assertEquals(1, viewModel.uiState.value.enteredDigits)
        viewModel.onDigitEntered('2')
        assertEquals(2, viewModel.uiState.value.enteredDigits)
    }

    @Test
    fun `backspace decrements entered digits count`() {
        viewModel.onDigitEntered('1')
        viewModel.onDigitEntered('2')
        viewModel.onBackspace()
        assertEquals(1, viewModel.uiState.value.enteredDigits)
    }

    @Test
    fun `backspace on empty pin does nothing`() {
        viewModel.onBackspace()
        assertEquals(0, viewModel.uiState.value.enteredDigits)
    }

    @Test
    fun `entering 4 correct digits auto-submits and emits Authenticated`() {
        correctPin.forEach { viewModel.onDigitEntered(it) }
        assertEquals(AuthOutcome.Authenticated, viewModel.uiState.value.authResult)
    }

    @Test
    fun `entering 4 wrong digits triggers shake`() {
        "9999".forEach { viewModel.onDigitEntered(it) }
        assertTrue(viewModel.uiState.value.isShaking)
    }

    @Test
    fun `wrong pin emits Spoofed outcome after shake delay`() {
        "9999".forEach { viewModel.onDigitEntered(it) }
        assertTrue(viewModel.uiState.value.isShaking)

        testDispatcher.scheduler.advanceTimeBy(401L)

        assertFalse(viewModel.uiState.value.isShaking)
        assertEquals(AuthOutcome.Spoofed, viewModel.uiState.value.authResult)
    }

    @Test
    fun `wrong pin clears entered digits after shake delay`() {
        "9999".forEach { viewModel.onDigitEntered(it) }
        testDispatcher.scheduler.advanceTimeBy(401L)
        assertEquals(0, viewModel.uiState.value.enteredDigits)
    }

    @Test
    fun `digits beyond 4 are ignored`() {
        "12345".forEach { viewModel.onDigitEntered(it) }
        // 5th digit is ignored; auth triggered at 4th digit with correct PIN
        assertEquals(AuthOutcome.Authenticated, viewModel.uiState.value.authResult)
    }

    @Test
    fun `biometric success emits Authenticated outcome`() {
        viewModel.onBiometricSuccess()
        assertEquals(AuthOutcome.Authenticated, viewModel.uiState.value.authResult)
    }

    @Test
    fun `biometric failure does not change auth result`() {
        viewModel.onBiometricFailure()
        assertNull(viewModel.uiState.value.authResult)
    }

    @Test
    fun `biometric failure after digit entry does not clear entered digits`() {
        viewModel.onDigitEntered('1')
        viewModel.onDigitEntered('2')
        viewModel.onBiometricFailure()
        assertEquals(2, viewModel.uiState.value.enteredDigits)
    }
}
