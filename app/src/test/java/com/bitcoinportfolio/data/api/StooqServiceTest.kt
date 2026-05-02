package com.bitcoinportfolio.data.api

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.ZoneOffset

class StooqServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var service: StooqService

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        service = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .build()
            .create(StooqService::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `CSV is parsed into PricePoints with correct timestamps and close prices`() = runTest {
        val csv = """
            Date,Open,High,Low,Close,Volume
            2024-01-01,1820.50,1850.00,1810.00,1840.00,10000
            2024-01-02,1840.00,1860.00,1830.00,1855.00,12000
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(csv).setResponseCode(200))

        val body = service.getHistoricalData(from = "20240101", to = "20240102")
        val points = StooqCsvParser.parse(body)

        assertEquals(2, points.size)
        val expectedTs = LocalDate.of(2024, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expectedTs, points[0].timestampMs)
        assertEquals(1840.00, points[0].priceUsd)
        assertEquals(1855.00, points[1].priceUsd)
    }

    @Test
    fun `header line is skipped and only data rows are returned`() = runTest {
        val csv = "Date,Open,High,Low,Close,Volume\n2024-01-01,1820.50,1850.00,1810.00,1840.00,10000"
        mockWebServer.enqueue(MockResponse().setBody(csv).setResponseCode(200))

        val body = service.getHistoricalData(from = "20240101", to = "20240101")
        val points = StooqCsvParser.parse(body)

        assertEquals(1, points.size)
        assertEquals(1840.00, points[0].priceUsd)
    }

    @Test
    fun `malformed lines are skipped and valid rows are returned`() = runTest {
        val csv = """
            Date,Open,High,Low,Close,Volume
            2024-01-01,1820.50,1850.00,1810.00,1840.00,10000
            bad-data
            not,enough
            2024-01-02,1840.00,1860.00,1830.00,1855.00,12000
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(csv).setResponseCode(200))

        val body = service.getHistoricalData(from = "20240101", to = "20240102")
        val points = StooqCsvParser.parse(body)

        assertEquals(2, points.size)
        assertEquals(1840.00, points[0].priceUsd)
        assertEquals(1855.00, points[1].priceUsd)
    }

    @Test
    fun `empty response body returns empty list`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("").setResponseCode(200))

        val body = service.getHistoricalData(from = "20240101", to = "20240101")
        val points = StooqCsvParser.parse(body)

        assertTrue(points.isEmpty())
    }

    @Test
    fun `response with only header line returns empty list`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("Date,Open,High,Low,Close,Volume").setResponseCode(200))

        val body = service.getHistoricalData(from = "20240101", to = "20240101")
        val points = StooqCsvParser.parse(body)

        assertTrue(points.isEmpty())
    }
}
