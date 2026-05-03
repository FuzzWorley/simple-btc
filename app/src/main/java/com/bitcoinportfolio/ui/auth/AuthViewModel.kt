package com.bitcoinportfolio.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitcoinportfolio.domain.usecase.AuthResult
import com.bitcoinportfolio.domain.usecase.AuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCase: AuthUseCase,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val pinBuffer = StringBuilder()

    init {
        val available = runCatching {
            BiometricManager.from(context)
                .canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)
        _uiState.update { it.copy(biometricAvailable = available) }
    }

    fun onDigitEntered(digit: Char) {
        if (pinBuffer.length >= 4) return
        pinBuffer.append(digit)
        _uiState.update { it.copy(enteredDigits = pinBuffer.length) }
        if (pinBuffer.length == 4) {
            submitPin(pinBuffer.toString())
        }
    }

    fun onBackspace() {
        if (pinBuffer.isEmpty()) return
        pinBuffer.deleteCharAt(pinBuffer.length - 1)
        _uiState.update { it.copy(enteredDigits = pinBuffer.length) }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(authResult = AuthOutcome.Authenticated) }
    }

    fun onBiometricFailure() {
        // fall back to PIN silently — no state change needed
    }

    fun onReset() {
        authUseCase.reset()
        _uiState.update { it.copy(resetComplete = true) }
    }

    private fun submitPin(pin: String) {
        when (authUseCase.authenticateWithPin(pin)) {
            is AuthResult.Success -> _uiState.update { it.copy(authResult = AuthOutcome.Authenticated) }
            is AuthResult.Failure -> {
                _uiState.update { it.copy(isShaking = true) }
                viewModelScope.launch {
                    delay(400)
                    pinBuffer.clear()
                    _uiState.update {
                        it.copy(isShaking = false, enteredDigits = 0, authResult = AuthOutcome.Spoofed)
                    }
                }
            }
        }
    }
}

data class AuthUiState(
    val enteredDigits: Int = 0,
    val isShaking: Boolean = false,
    val biometricAvailable: Boolean = false,
    val authResult: AuthOutcome? = null,
    val resetComplete: Boolean = false,
)

sealed class AuthOutcome {
    object Authenticated : AuthOutcome()
    object Spoofed : AuthOutcome()
}
