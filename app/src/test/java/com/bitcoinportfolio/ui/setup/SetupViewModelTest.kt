package com.bitcoinportfolio.ui.setup

import com.bitcoinportfolio.domain.usecase.SetupPortfolioUseCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val useCase: SetupPortfolioUseCase = mockk()
    private lateinit var viewModel: SetupViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { useCase.execute(any(), any()) } just runs
        viewModel = SetupViewModel(useCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty btc amount does not produce an error`() {
        viewModel.onBtcAmountChanged("")
        assertNull(viewModel.uiState.value.btcAmountError)
    }

    @Test
    fun `non-numeric btc amount produces error`() {
        viewModel.onBtcAmountChanged("abc")
        assertNotNull(viewModel.uiState.value.btcAmountError)
    }

    @Test
    fun `negative btc amount produces error`() {
        viewModel.onBtcAmountChanged("-1.0")
        assertNotNull(viewModel.uiState.value.btcAmountError)
    }

    @Test
    fun `zero btc amount produces error`() {
        viewModel.onBtcAmountChanged("0")
        assertNotNull(viewModel.uiState.value.btcAmountError)
    }

    @Test
    fun `valid btc amount clears error`() {
        viewModel.onBtcAmountChanged("0.5")
        assertNull(viewModel.uiState.value.btcAmountError)
    }

    @Test
    fun `btc amount with more than 8 decimal places produces error`() {
        viewModel.onBtcAmountChanged("0.123456789")
        assertNotNull(viewModel.uiState.value.btcAmountError)
    }

    @Test
    fun `btc amount with exactly 8 decimal places is valid`() {
        viewModel.onBtcAmountChanged("0.12345678")
        assertNull(viewModel.uiState.value.btcAmountError)
    }

    @Test
    fun `non-digit pin input is ignored`() {
        viewModel.onPinChanged("12ab")
        assertEquals("", viewModel.uiState.value.pin)
    }

    @Test
    fun `pin longer than 4 digits is ignored`() {
        viewModel.onPinChanged("12345")
        assertEquals("", viewModel.uiState.value.pin)
    }

    @Test
    fun `valid 4-digit pin is accepted`() {
        viewModel.onPinChanged("1234")
        assertEquals("1234", viewModel.uiState.value.pin)
    }

    @Test
    fun `confirm pin mismatch shows error`() {
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("5678")
        assertNotNull(viewModel.uiState.value.confirmPinError)
    }

    @Test
    fun `matching confirm pin clears error`() {
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")
        assertNull(viewModel.uiState.value.confirmPinError)
    }

    @Test
    fun `confirm pin error updates when pin changes to match`() {
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("5678")
        viewModel.onPinChanged("5678")
        assertNull(viewModel.uiState.value.confirmPinError)
    }

    @Test
    fun `form is invalid when btc amount is empty`() {
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")
        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `form is invalid when pin is less than 4 digits`() {
        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("123")
        viewModel.onConfirmPinChanged("123")
        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `form is invalid when pins do not match`() {
        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("5678")
        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `form is invalid when btc amount has error`() {
        viewModel.onBtcAmountChanged("-1")
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")
        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `form is valid when all fields are correctly filled`() {
        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")
        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `onSubmit calls use case with correct arguments`() {
        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")

        viewModel.onSubmit()

        verify { useCase.execute(0.5, "1234") }
    }

    @Test
    fun `onSubmit sets isComplete after use case executes`() {
        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")

        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `onSubmit does nothing when form is invalid`() {
        viewModel.onSubmit()

        verify(exactly = 0) { useCase.execute(any(), any()) }
        assertFalse(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `isLoading is false after submit completes`() {
        viewModel.onBtcAmountChanged("1.0")
        viewModel.onPinChanged("0000")
        viewModel.onConfirmPinChanged("0000")

        viewModel.onSubmit()

        assertFalse(viewModel.uiState.value.isLoading)
    }
}
