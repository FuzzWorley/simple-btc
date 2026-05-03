package com.bitcoinportfolio.ui.portfolio.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bitcoinportfolio.domain.model.BitcoinPrice
import com.bitcoinportfolio.domain.model.Currency
import kotlinx.coroutines.delay

@Composable
fun BtcPriceDisplay(
    price: BitcoinPrice,
    currency: Currency,
    lastUpdatedMs: Long?,
    isStale: Boolean,
    modifier: Modifier = Modifier,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 1-second ticker so "Updated Xs ago" stays live without recomposing the whole screen
    LaunchedEffect(lastUpdatedMs) {
        while (true) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = when (currency) {
                Currency.USD -> "$%,.2f".format(price.usd)
                Currency.XAU -> "%.4f oz".format(price.btcInXau)
            },
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Bitcoin Price",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!isStale && lastUpdatedMs != null) {
            val seconds = ((nowMs - lastUpdatedMs) / 1_000).coerceAtLeast(0)
            Text(
                text = "Updated ${seconds}s ago",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
