package com.bitcoinportfolio.ui.auth

import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitcoinportfolio.R
import com.bitcoinportfolio.ui.theme.BitcoinOrange
import com.bitcoinportfolio.ui.theme.SurfaceCard
import com.bitcoinportfolio.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    onAuthResult: (isAuthenticated: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as FragmentActivity

    val biometricTitle = stringResource(R.string.auth_biometric_title)
    val biometricSubtitle = stringResource(R.string.auth_biometric_subtitle)
    val biometricNegative = stringResource(R.string.auth_biometric_negative)

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(biometricTitle)
            .setSubtitle(biometricSubtitle)
            .setNegativeButtonText(biometricNegative)
            .build()
    }

    val biometricPrompt = remember(activity) {
        BiometricPrompt(
            activity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.onBiometricFailure()
                }

                override fun onAuthenticationFailed() {
                    viewModel.onBiometricFailure()
                }
            }
        )
    }

    LaunchedEffect(uiState.biometricAvailable) {
        if (uiState.biometricAvailable) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    LaunchedEffect(uiState.authResult) {
        uiState.authResult?.let { outcome ->
            onAuthResult(outcome is AuthOutcome.Authenticated)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "₿",
            style = MaterialTheme.typography.displayLarge,
            color = BitcoinOrange
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.auth_enter_pin),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        PinDotIndicator(
            enteredDigits = uiState.enteredDigits,
            isShaking = uiState.isShaking
        )

        Spacer(Modifier.height(40.dp))

        NumPad(
            onDigit = viewModel::onDigitEntered,
            onBackspace = viewModel::onBackspace,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.biometricAvailable) {
            Spacer(Modifier.height(24.dp))
            val useBiometricLabel = stringResource(R.string.auth_use_biometric)
            TextButton(
                onClick = { biometricPrompt.authenticate(promptInfo) },
                modifier = Modifier.semantics { contentDescription = useBiometricLabel }
            ) {
                Text(text = useBiometricLabel, color = BitcoinOrange)
            }
        }
    }
}

@Composable
private fun PinDotIndicator(
    enteredDigits: Int,
    isShaking: Boolean,
    modifier: Modifier = Modifier
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isShaking) {
        if (isShaking) {
            repeat(4) { i ->
                shakeOffset.animateTo(if (i % 2 == 0) 12f else -12f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    Row(
        modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .semantics { contentDescription = "PIN indicator $enteredDigits" },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < enteredDigits) BitcoinOrange
                        else TextSecondary.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun NumPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    NumPadKey(
                        label = digit.toString(),
                        contentDesc = "Digit $digit",
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }
        // Bottom row: empty cell for alignment, 0, backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(Modifier.size(72.dp))
            NumPadKey(label = "0", contentDesc = "Digit 0", onClick = { onDigit('0') })
            NumPadKey(label = "⌫", contentDesc = "Backspace", onClick = onBackspace)
        }
    }
}

@Composable
private fun NumPadKey(
    label: String,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
