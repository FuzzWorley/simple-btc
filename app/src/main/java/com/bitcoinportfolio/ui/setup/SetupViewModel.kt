package com.bitcoinportfolio.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitcoinportfolio.domain.usecase.SetupPortfolioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val setupPortfolioUseCase: SetupPortfolioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onBtcAmountChanged(value: String) {
        _uiState.update { it.copy(btcAmount = value, btcAmountError = validateBtcAmount(value)) }
    }

    fun onPinChanged(value: String) {
        if (value.any { !it.isDigit() } || value.length > 4) return
        _uiState.update { state ->
            val confirmError = if (state.confirmPin.isNotEmpty()) validatePinMatch(value, state.confirmPin) else null
            state.copy(pin = value, confirmPinError = confirmError)
        }
    }

    fun onConfirmPinChanged(value: String) {
        if (value.any { !it.isDigit() } || value.length > 4) return
        _uiState.update { state ->
            state.copy(confirmPin = value, confirmPinError = validatePinMatch(state.pin, value))
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.isFormValid) return
        val amount = state.btcAmount.toDouble()
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            setupPortfolioUseCase.execute(amount, state.pin)
            _uiState.update { it.copy(isLoading = false, isComplete = true) }
        }
    }

    private fun validateBtcAmount(value: String): String? {
        if (value.isEmpty()) return null
        val amount = value.toDoubleOrNull() ?: return "invalid"
        if (amount <= 0) return "invalid"
        val decimalPart = value.substringAfter('.', "")
        if (decimalPart.length > 8) return "invalid"
        return null
    }

    private fun validatePinMatch(pin: String, confirm: String): String? =
        if (pin != confirm) "mismatch" else null
}

data class SetupUiState(
    val btcAmount: String = "",
    val pin: String = "",
    val confirmPin: String = "",
    val btcAmountError: String? = null,
    val confirmPinError: String? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false
) {
    val isFormValid: Boolean
        get() = btcAmountError == null && btcAmount.isNotEmpty() &&
                pin.length == 4 && confirmPin.length == 4 &&
                confirmPinError == null
}
