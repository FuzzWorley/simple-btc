package com.bitcoinportfolio.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitcoinportfolio.R
import com.bitcoinportfolio.ui.theme.BitcoinOrange
import com.bitcoinportfolio.ui.theme.SurfaceCard
import com.bitcoinportfolio.ui.theme.TextSecondary

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onSetupComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Text(
            text = "₿",
            style = MaterialTheme.typography.displayLarge,
            color = BitcoinOrange
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(48.dp))

        OutlinedTextField(
            value = uiState.btcAmount,
            onValueChange = viewModel::onBtcAmountChanged,
            label = { Text(stringResource(R.string.setup_btc_amount_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = uiState.btcAmountError != null,
            supportingText = if (uiState.btcAmountError != null) {
                { Text(stringResource(R.string.setup_amount_error)) }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "BTC amount input" },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BitcoinOrange,
                focusedLabelColor = BitcoinOrange,
                cursorColor = BitcoinOrange
            )
        )

        Spacer(Modifier.height(24.dp))

        PinDotField(
            value = uiState.pin,
            onValueChange = viewModel::onPinChanged,
            label = stringResource(R.string.setup_pin_label),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        PinDotField(
            value = uiState.confirmPin,
            onValueChange = viewModel::onConfirmPinChanged,
            label = stringResource(R.string.setup_confirm_pin_label),
            isError = uiState.confirmPinError != null,
            errorText = if (uiState.confirmPinError != null) {
                stringResource(R.string.setup_pin_mismatch_error)
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(40.dp))

        val getStartedLabel = stringResource(R.string.setup_get_started)
        Button(
            onClick = viewModel::onSubmit,
            enabled = uiState.isFormValid && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { contentDescription = "Get started button" },
            colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(getStartedLabel)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PinDotField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.outline

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .semantics { contentDescription = label }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < value.length) BitcoinOrange
                                else TextSecondary.copy(alpha = 0.3f)
                            )
                    )
                    if (i < 3) Spacer(Modifier.width(20.dp))
                }
            }

            // Transparent overlay captures keyboard input; dots above provide visual feedback
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    if (new.all { it.isDigit() } && new.length <= 4) onValueChange(new)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0f)
            ) { /* no decoration — dots row is the visual */ }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
