package com.bitcoinportfolio.data.api

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CoinGeckoServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var service: CoinGeckoService

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        service = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(CoinGeckoService::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getCurrentPrice parses bitcoin usd value correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"bitcoin":{"usd":96240.00}}""")
                .setResponseCode(200)
        )

        val result = service.getCurrentPrice()

        assertEquals(96240.00, result.bitcoin.usd)
    }

    @Test
    fun `getPriceHistory parses prices list size and values`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"prices":[[1700000000000.0,42000.0],[1700086400000.0,43000.0]]}""")
                .setResponseCode(200)
        )

        val result = service.getPriceHistory(days = 1)

        assertEquals(2, result.prices.size)
        assertEquals(1700000000000.0, result.prices[0][0])
        assertEquals(42000.0, result.prices[0][1])
        assertEquals(43000.0, result.prices[1][1])
    }

    @Test
    fun `getPriceHistory throws HttpException on 429`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))

        val exception = runCatching { service.getPriceHistory(days = 1) }.exceptionOrNull()

        assertNotNull(exception)
        assertTrue(exception is HttpException)
        assertEquals(429, (exception as HttpException).code())
    }
}
