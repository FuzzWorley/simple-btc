package com.bitcoinportfolio.ui.portfolio.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.Portfolio

@Composable
fun PortfolioSummary(
    portfolio: Portfolio,
    currency: Currency,
    modifier: Modifier = Modifier,
) {
    val valueText = when (currency) {
        Currency.USD -> "$%,.2f".format(portfolio.valueUsd)
        Currency.XAU -> "%.4f oz".format(portfolio.valueXau)
    }
    val btcText = "%.8f BTC".format(portfolio.btcAmount)

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "$valueText  ·  $btcText",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Portfolio Value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
