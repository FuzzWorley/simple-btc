package com.bitcoinportfolio.ui.setup

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.bitcoinportfolio.domain.usecase.SetupPortfolioUseCase
import com.bitcoinportfolio.ui.theme.BitcoinPortfolioTheme
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val useCase: SetupPortfolioUseCase = mockk()
    private lateinit var viewModel: SetupViewModel

    @Before
    fun setUp() {
        every { useCase.execute(any(), any()) } just runs
        viewModel = SetupViewModel(useCase)
    }

    @Test
    fun getStartedButton_isDisabled_initially() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                SetupScreen(onSetupComplete = {}, viewModel = viewModel)
            }
        }
        composeRule.onNodeWithContentDescription("Get started button").assertIsNotEnabled()
    }

    @Test
    fun getStartedButton_isEnabled_whenFormIsValid() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                SetupScreen(onSetupComplete = {}, viewModel = viewModel)
            }
        }

        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Get started button").assertIsEnabled()
    }

    @Test
    fun getStartedButton_click_triggersOnSetupComplete() {
        var completed = false
        composeRule.setContent {
            BitcoinPortfolioTheme {
                SetupScreen(onSetupComplete = { completed = true }, viewModel = viewModel)
            }
        }

        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("1234")

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Get started button").performClick()
        composeRule.waitForIdle()

        assertTrue(completed)
    }

    @Test
    fun pinMismatchError_isShown_whenPinsDoNotMatch() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                SetupScreen(onSetupComplete = {}, viewModel = viewModel)
            }
        }

        viewModel.onPinChanged("1234")
        viewModel.onConfirmPinChanged("5678")

        composeRule.waitForIdle()
        composeRule.onNodeWithText("PINs do not match").assertIsDisplayed()
    }

    @Test
    fun getStartedButton_remainsDisabled_whenPinsTooShort() {
        composeRule.setContent {
            BitcoinPortfolioTheme {
                SetupScreen(onSetupComplete = {}, viewModel = viewModel)
            }
        }

        viewModel.onBtcAmountChanged("0.5")
        viewModel.onPinChanged("12")
        viewModel.onConfirmPinChanged("12")

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Get started button").assertIsNotEnabled()
    }
}
