package com.bitcoinportfolio.ui.portfolio

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitcoinportfolio.domain.model.ChartMode
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.ui.portfolio.components.BtcPriceDisplay
import com.bitcoinportfolio.ui.portfolio.components.EditAmountSheet
import com.bitcoinportfolio.ui.portfolio.components.PortfolioSummary
import com.bitcoinportfolio.ui.portfolio.components.PriceChart
import com.bitcoinportfolio.ui.portfolio.components.StaleDataBanner
import com.bitcoinportfolio.ui.portfolio.components.TimeRangeSelector

private val BitcoinOrange = Color(0xFFF7931A)

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pressing back on portfolio exits the app — no route to go back to
    BackHandler { /* consume: let the system handle app backgrounding */ }

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (uiState.isLoading && uiState.portfolio == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BitcoinOrange)
        }
    } else if (uiState.error != null && uiState.portfolio == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = uiState.error!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
    } else if (isLandscape) {
        LandscapePortfolioContent(uiState = uiState, viewModel = viewModel)
    } else {
        PortraitPortfolioContent(uiState = uiState, viewModel = viewModel)
    }

    if (uiState.isEditingAmount) {
        EditAmountSheet(
            currentAmount = uiState.portfolio?.btcAmount ?: 0.0,
            onConfirm = viewModel::onEditAmountConfirmed,
            onDismiss = viewModel::onEditAmountDismissed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitPortfolioContent(
    uiState: PortfolioUiState,
    viewModel: PortfolioViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "₿ Portfolio",
                    style = MaterialTheme.typography.titleLarge,
                    color = BitcoinOrange,
                )
            },
            actions = {
                CurrencyToggle(
                    selected = uiState.selectedCurrency,
                    onSelect = viewModel::onCurrencyToggle,
                    modifier = Modifier.padding(end = 8.dp),
                )
                IconButton(onClick = viewModel::onEditAmountRequested) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit BTC amount",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        ChartModeToggle(
            selected = uiState.selectedChartMode,
            onSelect = viewModel::onChartModeToggle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        PriceChart(
            pricePoints = uiState.priceHistory,
            chartMode = uiState.selectedChartMode,
            currency = uiState.selectedCurrency,
            btcAmount = uiState.portfolio?.btcAmount ?: 0.0,
            timeRange = uiState.selectedTimeRange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        TimeRangeSelector(
            selectedRange = uiState.selectedTimeRange,
            onRangeSelected = viewModel::onTimeRangeSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )

        if (uiState.isStale && uiState.staleAsOf != null) {
            StaleDataBanner(staleAsOf = uiState.staleAsOf)
        }

        uiState.portfolio?.let { portfolio ->
            Spacer(Modifier.height(12.dp))
            BtcPriceDisplay(
                price = portfolio.currentPrice,
                currency = uiState.selectedCurrency,
                lastUpdatedMs = uiState.lastUpdatedMs,
                isStale = uiState.isStale,
            )
            Spacer(Modifier.height(12.dp))
            PortfolioSummary(
                portfolio = portfolio,
                currency = uiState.selectedCurrency,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LandscapePortfolioContent(
    uiState: PortfolioUiState,
    viewModel: PortfolioViewModel,
) {
    Box(Modifier.fillMaxSize()) {
        PriceChart(
            pricePoints = uiState.priceHistory,
            chartMode = uiState.selectedChartMode,
            currency = uiState.selectedCurrency,
            btcAmount = uiState.portfolio?.btcAmount ?: 0.0,
            timeRange = uiState.selectedTimeRange,
            modifier = Modifier.fillMaxSize(),
        )

        // Top-left: chart mode toggle
        ChartModeToggle(
            selected = uiState.selectedChartMode,
            onSelect = viewModel::onChartModeToggle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )

        // Top-right: current price + currency toggle + edit button
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            uiState.portfolio?.let { portfolio ->
                Text(
                    text = when (uiState.selectedCurrency) {
                        Currency.USD -> "$%,.0f".format(portfolio.currentPrice.usd)
                        Currency.XAU -> "%.4f oz".format(portfolio.currentPrice.btcInXau)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            CurrencyToggle(
                selected = uiState.selectedCurrency,
                onSelect = viewModel::onCurrencyToggle,
                modifier = Modifier.padding(start = 8.dp),
            )
            IconButton(onClick = viewModel::onEditAmountRequested) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit BTC amount",
                    tint = Color.White,
                )
            }
        }

        // Bottom: time range selector
        TimeRangeSelector(
            selectedRange = uiState.selectedTimeRange,
            onRangeSelected = viewModel::onTimeRangeSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        // Stale overlay
        if (uiState.isStale && uiState.staleAsOf != null) {
            StaleDataBanner(
                staleAsOf = uiState.staleAsOf,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyToggle(
    selected: Currency,
    onSelect: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        Currency.entries.forEachIndexed { index, currency ->
            SegmentedButton(
                selected = selected == currency,
                onClick = { onSelect(currency) },
                shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size),
                label = { Text(currency.name) },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BitcoinOrange,
                    activeContentColor = Color.Black,
                ),
            )
        }
    }
}

@Composable
private fun ChartModeToggle(
    selected: ChartMode,
    onSelect: (ChartMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        ChartMode.entries.forEach { mode ->
            val isSelected = mode == selected
            androidx.compose.material3.TextButton(
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = if (isSelected) BitcoinOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = when (mode) {
                        ChartMode.PORTFOLIO_VALUE -> "Portfolio Value"
                        ChartMode.BTC_PRICE -> "BTC Price"
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
