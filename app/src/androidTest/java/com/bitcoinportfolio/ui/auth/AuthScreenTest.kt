package com.bitcoinportfolio.ui.auth

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.bitcoinportfolio.data.secure.PinHasher
import com.bitcoinportfolio.data.secure.SecureStorage
import com.bitcoinportfolio.domain.usecase.AuthUseCase
import com.bitcoinportfolio.ui.theme.BitcoinPortfolioTheme
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val secureStorage: SecureStorage = mockk()
    private val pinHasher = PinHasher()
    private val authUseCase = AuthUseCase(secureStorage, pinHasher)
    private lateinit var viewModel: AuthViewModel

    private val correctPin = "1234"
    private lateinit var salt: String

    @Before
    fun setUp() {
        salt = pinHasher.generateSalt()
        every { secureStorage.getPinSalt() } returns salt
        every { secureStorage.getPinHash() } returns pinHasher.hash(correctPin, salt)
        every { secureStorage.saveSpoofAmount(any()) } just runs
        viewModel = AuthViewModel(authUseCase, context)
    }

    @Test
    fun numpad_renders_all_digit_keys() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                AuthScreen(onAuthResult = {}, viewModel = viewModel)
            }
        }

        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { digit ->
            composeRule.onNodeWithContentDescription("Digit $digit").assertExists()
        }
        composeRule.onNodeWithContentDescription("Backspace").assertExists()
    }

    @Test
    fun tapping_digit_updates_pin_indicator() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                AuthScreen(onAuthResult = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithContentDescription("Digit 1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("PIN indicator 1").assertExists()

        composeRule.onNodeWithContentDescription("Digit 2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("PIN indicator 2").assertExists()
    }

    @Test
    fun backspace_removes_last_digit() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                AuthScreen(onAuthResult = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithContentDescription("Digit 1").performClick()
        composeRule.onNodeWithContentDescription("Digit 2").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Backspace").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("PIN indicator 1").assertExists()
    }

    @Test
    fun correct_pin_triggers_onAuthResult_with_true() {
        var authResult: Boolean? = null
        composeRule.setContent {
            BitcoinPortfolioTheme {
                AuthScreen(onAuthResult = { authResult = it }, viewModel = viewModel)
            }
        }

        correctPin.forEach { digit ->
            composeRule.onNodeWithContentDescription("Digit $digit").performClick()
        }
        composeRule.waitForIdle()

        assertNotNull(authResult)
        assertEquals(true, authResult)
    }

    @Test
    fun wrong_pin_triggers_onAuthResult_with_false() {
        var authResult: Boolean? = null
        composeRule.setContent {
            BitcoinPortfolioTheme {
                AuthScreen(onAuthResult = { authResult = it }, viewModel = viewModel)
            }
        }

        "9999".forEach { digit ->
            composeRule.onNodeWithContentDescription("Digit $digit").performClick()
        }
        // Wait for the 400ms shake delay plus some buffer
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitForIdle()

        assertNotNull(authResult)
        assertEquals(false, authResult)
    }

    @Test
    fun biometric_button_not_shown_when_unavailable() {
        // On most emulators biometric hardware is unavailable, so the button should not appear
        composeRule.setContent {
            BitcoinPortfolioTheme {
                AuthScreen(onAuthResult = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("Use Biometric").assertDoesNotExist()
    }

    @Test
    fun enter_pin_label_is_shown() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                AuthScreen(onAuthResult = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("Enter PIN").assertExists()
    }
}
