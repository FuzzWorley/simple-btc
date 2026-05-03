package com.bitcoinportfolio.ui.portfolio.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val BitcoinOrange = Color(0xFFF7931A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAmountSheet(
    currentAmount: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Pre-fill with current amount, stripping trailing zeros for readability
    var input by remember {
        mutableStateOf(
            currentAmount.toBigDecimal().stripTrailingZeros().toPlainString()
        )
    }

    val parsedAmount = input.toDoubleOrNull()
    val isValid = parsedAmount != null
        && parsedAmount > 0
        && (parsedAmount.toBigDecimal().stripTrailingZeros().scale() <= 8)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Edit BTC Amount",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("BTC Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = input.isNotEmpty() && parsedAmount == null,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { parsedAmount?.let { onConfirm(it) } },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
            ) {
                Text("Save", color = Color.Black)
            }
        }
    }
}
