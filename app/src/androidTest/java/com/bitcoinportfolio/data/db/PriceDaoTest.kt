package com.bitcoinportfolio.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bitcoinportfolio.data.db.entity.CurrentPriceEntity
import com.bitcoinportfolio.data.db.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PriceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PriceDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.priceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- CurrentPrice ---

    @Test
    fun upsertCurrentPrice_replacesExistingRow() = runTest {
        val first = CurrentPriceEntity(btcUsd = 90_000.0, xauUsd = 3_000.0, btcXau = 30.0, fetchedAtMs = 1_000L)
        dao.upsertCurrentPrice(first)

        val second = CurrentPriceEntity(btcUsd = 95_000.0, xauUsd = 3_100.0, btcXau = 30.6, fetchedAtMs = 2_000L)
        dao.upsertCurrentPrice(second)

        val result = dao.observeCurrentPrice().first()
        assertNotNull(result)
        assertEquals(95_000.0, result!!.btcUsd, 0.0)
        assertEquals(2_000L, result.fetchedAtMs)
    }

    @Test
    fun observeCurrentPrice_emitsNullWhenEmpty() = runTest {
        val result = dao.observeCurrentPrice().first()
        assertNull(result)
    }

    @Test
    fun observeCurrentPrice_emitsInsertedRow() = runTest {
        val entity = CurrentPriceEntity(btcUsd = 96_240.0, xauUsd = 3_050.0, btcXau = 31.55, fetchedAtMs = 5_000L)
        dao.upsertCurrentPrice(entity)

        val result = dao.observeCurrentPrice().first()
        assertNotNull(result)
        assertEquals(96_240.0, result!!.btcUsd, 0.0)
        assertEquals(3_050.0, result.xauUsd, 0.0)
        assertEquals(31.55, result.btcXau, 0.001)
    }

    // --- PriceHistory ---

    @Test
    fun observeHistory_returnsRowsInTimestampAscendingOrder() = runTest {
        val entities = listOf(
            historyRow(symbol = "BTC_USD", timeRange = "ONE_MONTH", timestampMs = 300L, price = 3.0),
            historyRow(symbol = "BTC_USD", timeRange = "ONE_MONTH", timestampMs = 100L, price = 1.0),
            historyRow(symbol = "BTC_USD", timeRange = "ONE_MONTH", timestampMs = 200L, price = 2.0)
        )
        dao.insertHistory(entities)

        val result = dao.observeHistory("BTC_USD", "ONE_MONTH").first()
        assertEquals(3, result.size)
        assertEquals(100L, result[0].timestampMs)
        assertEquals(200L, result[1].timestampMs)
        assertEquals(300L, result[2].timestampMs)
    }

    @Test
    fun insertHistory_observeHistory_roundTrip() = runTest {
        val entities = listOf(
            historyRow("BTC_USD", "ONE_WEEK", 1_000L, 90_000.0),
            historyRow("BTC_USD", "ONE_WEEK", 2_000L, 91_000.0)
        )
        dao.insertHistory(entities)

        val result = dao.observeHistory("BTC_USD", "ONE_WEEK").first()
        assertEquals(2, result.size)
        assertEquals(90_000.0, result[0].price, 0.0)
        assertEquals(91_000.0, result[1].price, 0.0)
    }

    @Test
    fun clearHistory_removesOnlyMatchingSymbolAndRange() = runTest {
        dao.insertHistory(listOf(
            historyRow("BTC_USD", "ONE_MONTH", 1_000L, 90_000.0),
            historyRow("BTC_USD", "ONE_WEEK", 2_000L, 91_000.0),
            historyRow("XAU_USD", "ONE_MONTH", 3_000L, 3_000.0)
        ))

        dao.clearHistory("BTC_USD", "ONE_MONTH")

        val cleared = dao.observeHistory("BTC_USD", "ONE_MONTH").first()
        val kept1 = dao.observeHistory("BTC_USD", "ONE_WEEK").first()
        val kept2 = dao.observeHistory("XAU_USD", "ONE_MONTH").first()

        assertEquals(0, cleared.size)
        assertEquals(1, kept1.size)
        assertEquals(1, kept2.size)
    }

    @Test
    fun getLastFetchedAt_returnsNullWhenEmpty() = runTest {
        val result = dao.getLastFetchedAt("BTC_USD", "ONE_MONTH")
        assertNull(result)
    }

    @Test
    fun getLastFetchedAt_returnsMaxFetchedAtMs() = runTest {
        dao.insertHistory(listOf(
            historyRow("BTC_USD", "ONE_MONTH", 1_000L, 90_000.0, fetchedAtMs = 500L),
            historyRow("BTC_USD", "ONE_MONTH", 2_000L, 91_000.0, fetchedAtMs = 999L),
            historyRow("BTC_USD", "ONE_MONTH", 3_000L, 92_000.0, fetchedAtMs = 750L)
        ))

        val result = dao.getLastFetchedAt("BTC_USD", "ONE_MONTH")
        assertEquals(999L, result)
    }

    // --- helpers ---

    private fun historyRow(
        symbol: String,
        timeRange: String,
        timestampMs: Long,
        price: Double,
        fetchedAtMs: Long = 1_000L
    ) = PriceHistoryEntity(
        symbol = symbol,
        timeRange = timeRange,
        timestampMs = timestampMs,
        price = price,
        fetchedAtMs = fetchedAtMs
    )
}
