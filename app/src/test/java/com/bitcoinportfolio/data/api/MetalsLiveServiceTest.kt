package com.bitcoinportfolio.data.api

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MetalsLiveServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var service: MetalsLiveService

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        service = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(MetalsLiveService::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getSpotPrices extracts gold price correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"gold":3000.00,"silver":30.50}]""")
                .setResponseCode(200)
        )

        val result = service.getSpotPrices()

        assertEquals(1, result.size)
        assertEquals(3000.00, result[0].gold)
    }

    @Test
    fun `getSpotPrices handles null gold value`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"gold":null,"silver":30.50}]""")
                .setResponseCode(200)
        )

        val result = service.getSpotPrices()

        assertEquals(1, result.size)
        assertNull(result[0].gold)
    }

    @Test
    fun `getSpotPrices handles multiple entries in array`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"gold":3000.00},{"gold":3001.00}]""")
                .setResponseCode(200)
        )

        val result = service.getSpotPrices()

        assertEquals(2, result.size)
        assertEquals(3000.00, result[0].gold)
        assertEquals(3001.00, result[1].gold)
    }
}
