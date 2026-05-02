package com.bitcoinportfolio.domain.usecase

import app.cash.turbine.test
import com.bitcoinportfolio.domain.model.BitcoinPrice
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.repository.PriceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetCurrentPriceUseCaseTest {

    private val repository: PriceRepository = mockk()
    private val useCase = GetCurrentPriceUseCase(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        val price = fakeBitcoinPrice()
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Success(price))

        useCase().test {
            val item = awaitItem() as DataResult.Success
            assertEquals(price.usd, item.data.usd)
            awaitComplete()
        }

        verify(exactly = 1) { repository.observeCurrentPrice() }
    }

    @Test
    fun `invoke passes through Loading state`() = runTest {
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Loading)

        useCase().test {
            assertEquals(DataResult.Loading, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `invoke passes through Error state`() = runTest {
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Error("network error"))

        useCase().test {
            val item = awaitItem() as DataResult.Error
            assertEquals("network error", item.message)
            awaitComplete()
        }
    }

    @Test
    fun `invoke preserves isStale flag`() = runTest {
        val price = fakeBitcoinPrice(isStale = true)
        val staleTs = 12345L
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Success(price, isStale = true, staleAsOf = staleTs))

        useCase().test {
            val item = awaitItem() as DataResult.Success
            assertEquals(true, item.isStale)
            assertEquals(staleTs, item.staleAsOf)
            awaitComplete()
        }
    }

    private fun fakeBitcoinPrice(isStale: Boolean = false) = BitcoinPrice(
        usd = 96_000.0,
        xauPerOz = 3_000.0,
        btcInXau = 32.0,
        fetchedAtMs = System.currentTimeMillis(),
        isStale = isStale,
    )
}
