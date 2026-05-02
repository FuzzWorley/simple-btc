package com.bitcoinportfolio.data.repository

import com.bitcoinportfolio.data.api.CoinGeckoService
import com.bitcoinportfolio.data.api.MetalsLiveService
import com.bitcoinportfolio.data.api.StooqCsvParser
import com.bitcoinportfolio.data.api.StooqService
import com.bitcoinportfolio.data.db.PriceDao
import com.bitcoinportfolio.data.db.entity.CurrentPriceEntity
import com.bitcoinportfolio.data.db.entity.PriceHistoryEntity
import com.bitcoinportfolio.domain.model.BitcoinPrice
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.PricePoint
import com.bitcoinportfolio.domain.model.TimeRange
import com.bitcoinportfolio.domain.repository.PriceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceRepositoryImpl @Inject constructor(
    private val dao: PriceDao,
    private val coinGeckoService: CoinGeckoService,
    private val metalsLiveService: MetalsLiveService,
    private val stooqService: StooqService,
) : PriceRepository {

    companion object {
        private const val STALE_CURRENT_MS = 5 * 60 * 1000L
        private const val STALE_HISTORY_MS = 24 * 60 * 60 * 1000L
        const val SYMBOL_BTC_USD = "BTC_USD"
        const val SYMBOL_XAU_USD = "XAU_USD"
    }

    override fun observeCurrentPrice(): Flow<DataResult<BitcoinPrice>> = channelFlow {
        send(DataResult.Loading)

        launch {
            try { refreshCurrentPrice() } catch (_: Exception) { }
        }

        dao.observeCurrentPrice().collect { entity ->
            if (entity != null) {
                val isStale = System.currentTimeMillis() - entity.fetchedAtMs > STALE_CURRENT_MS
                send(DataResult.Success(entity.toBitcoinPrice(isStale), isStale, if (isStale) entity.fetchedAtMs else null))
            }
        }
    }

    override fun observePriceHistory(
        timeRange: TimeRange,
        currency: Currency,
    ): Flow<DataResult<List<PricePoint>>> = channelFlow {
        send(DataResult.Loading)

        launch {
            try { refreshHistory(timeRange) } catch (_: Exception) { }
        }

        if (currency == Currency.USD) {
            dao.observeHistory(SYMBOL_BTC_USD, timeRange.name).collect { entities ->
                val isStale = entities.isStaleHistory()
                val points = entities.map { PricePoint(it.timestampMs, it.price) }
                if (points.isNotEmpty()) {
                    send(DataResult.Success(points, isStale, entities.firstOrNull()?.fetchedAtMs?.takeIf { isStale }))
                }
            }
        } else {
            combine(
                dao.observeHistory(SYMBOL_BTC_USD, timeRange.name),
                dao.observeHistory(SYMBOL_XAU_USD, timeRange.name),
            ) { btcEntities, xauEntities -> btcEntities to xauEntities }
                .collect { (btcEntities, xauEntities) ->
                    if (btcEntities.isNotEmpty() && xauEntities.isNotEmpty()) {
                        val isStale = btcEntities.isStaleHistory() || xauEntities.isStaleHistory()
                        val xauPoints = xauEntities.map { PricePoint(it.timestampMs, it.price) }
                        val aligned = btcEntities.mapNotNull { btcEntity ->
                            val nearestXau = nearestXauPoint(btcEntity.timestampMs, xauPoints)
                                ?: return@mapNotNull null
                            PricePoint(btcEntity.timestampMs, btcEntity.price / nearestXau.priceUsd)
                        }
                        if (aligned.isNotEmpty()) {
                            send(DataResult.Success(aligned, isStale, btcEntities.firstOrNull()?.fetchedAtMs?.takeIf { isStale }))
                        }
                    }
                }
        }
    }

    override suspend fun refreshCurrentPrice() = coroutineScope {
        val btcDeferred = async { coinGeckoService.getCurrentPrice() }
        val xauDeferred = async {
            metalsLiveService.getSpotPrices().firstOrNull()?.gold
        }
        val btcUsd = btcDeferred.await().bitcoin.usd
        val xauUsd = xauDeferred.await() ?: return@coroutineScope
        dao.upsertCurrentPrice(
            CurrentPriceEntity(
                btcUsd = btcUsd,
                xauUsd = xauUsd,
                btcXau = btcUsd / xauUsd,
                fetchedAtMs = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun refreshHistory(timeRange: TimeRange) = coroutineScope {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val today = LocalDate.now(ZoneOffset.UTC)
        val from = today.minusDays(timeRange.days.toLong())
        val d1 = from.format(dateFormatter)
        val d2 = today.format(dateFormatter)
        val fetchedAt = System.currentTimeMillis()

        val btcDeferred = async {
            coinGeckoService.getPriceHistory(days = timeRange.days)
                .prices
                .map { PriceHistoryEntity(symbol = SYMBOL_BTC_USD, timeRange = timeRange.name, timestampMs = it[0].toLong(), price = it[1], fetchedAtMs = fetchedAt) }
        }
        val xauDeferred = async {
            try {
                val body = stooqService.getHistoricalData(from = d1, to = d2)
                StooqCsvParser.parse(body).map {
                    PriceHistoryEntity(symbol = SYMBOL_XAU_USD, timeRange = timeRange.name, timestampMs = it.timestampMs, price = it.priceUsd, fetchedAtMs = fetchedAt)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val btcEntities = btcDeferred.await()
        val xauEntities = xauDeferred.await()

        if (btcEntities.isNotEmpty()) {
            dao.clearHistory(SYMBOL_BTC_USD, timeRange.name)
            dao.insertHistory(btcEntities)
        }
        if (xauEntities.isNotEmpty()) {
            dao.clearHistory(SYMBOL_XAU_USD, timeRange.name)
            dao.insertHistory(xauEntities)
        }
    }

    // --- helpers ---

    private fun CurrentPriceEntity.toBitcoinPrice(isStale: Boolean) = BitcoinPrice(
        usd = btcUsd,
        xauPerOz = xauUsd,
        btcInXau = btcXau,
        fetchedAtMs = fetchedAtMs,
        isStale = isStale,
    )

    private fun List<PriceHistoryEntity>.isStaleHistory(): Boolean {
        val lastFetched = maxOfOrNull { it.fetchedAtMs } ?: return true
        return System.currentTimeMillis() - lastFetched > STALE_HISTORY_MS
    }

    private fun nearestXauPoint(targetMs: Long, xauPoints: List<PricePoint>): PricePoint? {
        if (xauPoints.isEmpty()) return null
        var lo = 0
        var hi = xauPoints.lastIndex
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (xauPoints[mid].timestampMs < targetMs) lo = mid + 1 else hi = mid
        }
        val candidates = listOfNotNull(
            xauPoints.getOrNull(lo - 1),
            xauPoints.getOrNull(lo),
        )
        val nearest = candidates.minByOrNull { kotlin.math.abs(it.timestampMs - targetMs) }
            ?: return null
        // Discard if more than 24 hours away
        return if (kotlin.math.abs(nearest.timestampMs - targetMs) <= 24 * 60 * 60 * 1000L) nearest else null
    }
}
