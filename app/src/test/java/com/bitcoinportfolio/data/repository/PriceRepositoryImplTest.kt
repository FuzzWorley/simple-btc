package com.bitcoinportfolio.data.repository

import app.cash.turbine.test
import com.bitcoinportfolio.data.api.CoinGeckoService
import com.bitcoinportfolio.data.api.MetalsLiveService
import com.bitcoinportfolio.data.api.StooqService
import com.bitcoinportfolio.data.db.PriceDao
import com.bitcoinportfolio.data.db.entity.CurrentPriceEntity
import com.bitcoinportfolio.data.db.entity.PriceHistoryEntity
import com.bitcoinportfolio.data.repository.PriceRepositoryImpl.Companion.SYMBOL_BTC_USD
import com.bitcoinportfolio.data.repository.PriceRepositoryImpl.Companion.SYMBOL_XAU_USD
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.TimeRange
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PriceRepositoryImplTest {

    private val dao: PriceDao = mockk(relaxed = true)
    private val coinGeckoService: CoinGeckoService = mockk()
    private val metalsLiveService: MetalsLiveService = mockk()
    private val stooqService: StooqService = mockk()

    private lateinit var repo: PriceRepositoryImpl

    private val currentPriceFlow = MutableStateFlow<CurrentPriceEntity?>(null)
    private val btcHistoryFlow = MutableStateFlow<List<PriceHistoryEntity>>(emptyList())
    private val xauHistoryFlow = MutableStateFlow<List<PriceHistoryEntity>>(emptyList())

    @BeforeEach
    fun setUp() {
        every { dao.observeCurrentPrice() } returns currentPriceFlow
        every { dao.observeHistory(SYMBOL_BTC_USD, any()) } returns btcHistoryFlow
        every { dao.observeHistory(SYMBOL_XAU_USD, any()) } returns xauHistoryFlow

        repo = PriceRepositoryImpl(dao, coinGeckoService, metalsLiveService, stooqService)
    }

    // --- observeCurrentPrice ---

    @Test
    fun `observeCurrentPrice emits Loading then Success when cache is populated`() = runTest {
        val entity = freshCurrentPriceEntity()
        coEvery { coinGeckoService.getCurrentPrice() } throws RuntimeException("network off")

        repo.observeCurrentPrice().test {
            assertEquals(DataResult.Loading, awaitItem())

            currentPriceFlow.value = entity

            val result = awaitItem()
            assertTrue(result is DataResult.Success)
            val success = result as DataResult.Success
            assertEquals(96_000.0, success.data.usd)
            assertFalse(success.isStale)
            assertNull(success.staleAsOf)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCurrentPrice marks data stale when fetchedAtMs is older than 5 minutes`() = runTest {
        val staleTs = System.currentTimeMillis() - (6 * 60 * 1000L)
        val entity = freshCurrentPriceEntity(fetchedAtMs = staleTs)
        coEvery { coinGeckoService.getCurrentPrice() } throws RuntimeException("network off")

        repo.observeCurrentPrice().test {
            assertEquals(DataResult.Loading, awaitItem())
            currentPriceFlow.value = entity

            val result = awaitItem() as DataResult.Success
            assertTrue(result.isStale)
            assertEquals(staleTs, result.staleAsOf)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeCurrentPrice with fresh timestamp is not stale`() = runTest {
        val ts = System.currentTimeMillis() - (4 * 60 * 1000L)
        val entity = freshCurrentPriceEntity(fetchedAtMs = ts)
        coEvery { coinGeckoService.getCurrentPrice() } throws RuntimeException("network off")

        repo.observeCurrentPrice().test {
            assertEquals(DataResult.Loading, awaitItem())
            currentPriceFlow.value = entity

            val result = awaitItem() as DataResult.Success
            assertFalse(result.isStale)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- observePriceHistory USD ---

    @Test
    fun `observePriceHistory USD emits Success with BTC_USD points`() = runTest {
        coEvery { coinGeckoService.getPriceHistory(any()) } throws RuntimeException("network off")

        repo.observePriceHistory(TimeRange.ONE_MONTH, Currency.USD).test {
            assertEquals(DataResult.Loading, awaitItem())

            val now = System.currentTimeMillis()
            btcHistoryFlow.value = listOf(
                historyRow(SYMBOL_BTC_USD, TimeRange.ONE_MONTH, 1_000L, 90_000.0, now),
                historyRow(SYMBOL_BTC_USD, TimeRange.ONE_MONTH, 2_000L, 91_000.0, now),
            )

            val result = awaitItem() as DataResult.Success
            assertEquals(2, result.data.size)
            assertEquals(90_000.0, result.data[0].priceUsd)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- observePriceHistory XAU ---

    @Test
    fun `observePriceHistory XAU divides BTC by nearest XAU point`() = runTest {
        coEvery { coinGeckoService.getPriceHistory(any()) } throws RuntimeException("network off")
        coEvery { stooqService.getHistoricalData(any(), any(), any(), any()) } throws RuntimeException("network off")

        repo.observePriceHistory(TimeRange.ONE_MONTH, Currency.XAU).test {
            assertEquals(DataResult.Loading, awaitItem())

            val now = System.currentTimeMillis()
            btcHistoryFlow.value = listOf(
                historyRow(SYMBOL_BTC_USD, TimeRange.ONE_MONTH, 1_000L, 96_000.0, now),
            )
            xauHistoryFlow.value = listOf(
                historyRow(SYMBOL_XAU_USD, TimeRange.ONE_MONTH, 1_000L, 3_000.0, now),
            )

            val result = awaitItem() as DataResult.Success
            assertEquals(1, result.data.size)
            assertEquals(32.0, result.data[0].priceUsd, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observePriceHistory XAU discards BTC points with no XAU within 24h`() = runTest {
        coEvery { coinGeckoService.getPriceHistory(any()) } throws RuntimeException("network off")
        coEvery { stooqService.getHistoricalData(any(), any(), any(), any()) } throws RuntimeException("network off")

        repo.observePriceHistory(TimeRange.ONE_MONTH, Currency.XAU).test {
            assertEquals(DataResult.Loading, awaitItem())

            val now = System.currentTimeMillis()
            btcHistoryFlow.value = listOf(
                historyRow(SYMBOL_BTC_USD, TimeRange.ONE_MONTH, 0L, 96_000.0, now),
            )
            // XAU point is 25 hours away — should be discarded
            xauHistoryFlow.value = listOf(
                historyRow(SYMBOL_XAU_USD, TimeRange.ONE_MONTH, 25 * 60 * 60 * 1000L, 3_000.0, now),
            )

            // No Success emitted since the aligned list is empty — only Loading
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- stale threshold boundary ---

    @Test
    fun `stale threshold 4m59s is not stale`() = runTest {
        val ts = System.currentTimeMillis() - (4 * 60 * 1000L + 59_000L)
        val entity = freshCurrentPriceEntity(fetchedAtMs = ts)
        coEvery { coinGeckoService.getCurrentPrice() } throws RuntimeException("network off")

        repo.observeCurrentPrice().test {
            assertEquals(DataResult.Loading, awaitItem())
            currentPriceFlow.value = entity
            val result = awaitItem() as DataResult.Success
            assertFalse(result.isStale)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stale threshold 5m01s is stale`() = runTest {
        val ts = System.currentTimeMillis() - (5 * 60 * 1000L + 1_000L)
        val entity = freshCurrentPriceEntity(fetchedAtMs = ts)
        coEvery { coinGeckoService.getCurrentPrice() } throws RuntimeException("network off")

        repo.observeCurrentPrice().test {
            assertEquals(DataResult.Loading, awaitItem())
            currentPriceFlow.value = entity
            val result = awaitItem() as DataResult.Success
            assertTrue(result.isStale)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- refreshCurrentPrice ---

    @Test
    fun `refreshCurrentPrice fetches in parallel and upserts to Room`() = runTest {
        val priceDto = mockk<com.bitcoinportfolio.data.api.dto.CoinGeckoPriceDto> {
            every { bitcoin.usd } returns 97_000.0
        }
        val metalsDto = mockk<com.bitcoinportfolio.data.api.dto.MetalsLiveDto> {
            every { gold } returns 3_100.0
        }
        coEvery { coinGeckoService.getCurrentPrice() } returns priceDto
        coEvery { metalsLiveService.getSpotPrices() } returns listOf(metalsDto)

        repo.refreshCurrentPrice()

        coVerify {
            dao.upsertCurrentPrice(
                match {
                    it.btcUsd == 97_000.0 &&
                        it.xauUsd == 3_100.0 &&
                        kotlin.math.abs(it.btcXau - (97_000.0 / 3_100.0)) < 0.001
                }
            )
        }
    }

    @Test
    fun `refreshCurrentPrice does nothing when gold price is null`() = runTest {
        val priceDto = mockk<com.bitcoinportfolio.data.api.dto.CoinGeckoPriceDto> {
            every { bitcoin.usd } returns 97_000.0
        }
        val metalsDto = mockk<com.bitcoinportfolio.data.api.dto.MetalsLiveDto> {
            every { gold } returns null
        }
        coEvery { coinGeckoService.getCurrentPrice() } returns priceDto
        coEvery { metalsLiveService.getSpotPrices() } returns listOf(metalsDto)

        repo.refreshCurrentPrice()

        coVerify(exactly = 0) { dao.upsertCurrentPrice(any()) }
    }

    // --- helpers ---

    private fun freshCurrentPriceEntity(
        fetchedAtMs: Long = System.currentTimeMillis(),
    ) = CurrentPriceEntity(
        btcUsd = 96_000.0,
        xauUsd = 3_000.0,
        btcXau = 32.0,
        fetchedAtMs = fetchedAtMs,
    )

    private fun historyRow(
        symbol: String,
        timeRange: TimeRange,
        timestampMs: Long,
        price: Double,
        fetchedAtMs: Long,
    ) = PriceHistoryEntity(
        symbol = symbol,
        timeRange = timeRange.name,
        timestampMs = timestampMs,
        price = price,
        fetchedAtMs = fetchedAtMs,
    )
}
