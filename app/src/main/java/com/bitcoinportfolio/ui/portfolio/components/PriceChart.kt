package com.bitcoinportfolio.ui.portfolio.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bitcoinportfolio.domain.model.ChartMode
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.PricePoint
import com.bitcoinportfolio.domain.model.TimeRange
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BitcoinOrange = Color(0xFFF7931A)

@Composable
fun PriceChart(
    pricePoints: List<PricePoint>,
    chartMode: ChartMode,
    currency: Currency,
    btcAmount: Double,
    timeRange: TimeRange,
    modifier: Modifier = Modifier,
) {
    if (pricePoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No chart data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
        return
    }

    val modelProducer = remember { CartesianChartModelProducer.build() }

    // Capture latest values for axis formatters without triggering chart recomposition
    val currentPoints by rememberUpdatedState(pricePoints)
    val currentCurrency by rememberUpdatedState(currency)
    val currentRange by rememberUpdatedState(timeRange)

    LaunchedEffect(pricePoints, chartMode, btcAmount) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = List(pricePoints.size) { it.toFloat() },
                    y = pricePoints.map { point ->
                        when (chartMode) {
                            ChartMode.PORTFOLIO_VALUE -> (point.priceUsd * btcAmount).toFloat()
                            ChartMode.BTC_PRICE -> point.priceUsd.toFloat()
                        }
                    },
                )
            }
        }
    }

    // Build line styling outside remember so rememberLine can key on these objects
    val lineFill = remember { LineCartesianLayer.LineFill.single(fill(BitcoinOrange)) }
    val areaFill = remember {
        LineCartesianLayer.AreaFill.single(
            fill(
                DynamicShader.verticalGradient(
                    arrayOf(BitcoinOrange.copy(alpha = 0.4f), BitcoinOrange.copy(alpha = 0f))
                )
            )
        )
    }

    val line = rememberLine(fill = lineFill, areaFill = areaFill)
    val lineLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(lines = listOf(line))
    )

    val startAxis = rememberStartAxis(
        valueFormatter = { value, _, _ ->
            when (currentCurrency) {
                Currency.USD -> "$%,.0f".format(value)
                Currency.XAU -> "%.2f oz".format(value)
            }
        },
    )

    val bottomAxis = rememberBottomAxis(
        valueFormatter = { value, _, _ ->
            val idx = value.toInt().coerceIn(0, currentPoints.lastIndex)
            formatAxisTimestamp(currentPoints[idx].timestampMs, currentRange)
        },
        itemPlacer = remember { HorizontalAxis.ItemPlacer.default() },
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            lineLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis,
            horizontalLayout = HorizontalLayout.fullWidth(),
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
        modifier = modifier,
    )
}

private fun formatAxisTimestamp(ms: Long, timeRange: TimeRange): String {
    val pattern = when (timeRange) {
        TimeRange.ONE_DAY -> "HH:mm"
        TimeRange.ONE_WEEK, TimeRange.ONE_MONTH -> "MMM d"
        else -> "MMM ''yy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
}
