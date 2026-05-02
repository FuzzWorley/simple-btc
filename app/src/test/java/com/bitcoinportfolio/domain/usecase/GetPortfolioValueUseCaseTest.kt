package com.bitcoinportfolio.domain.usecase

import app.cash.turbine.test
import com.bitcoinportfolio.domain.model.BitcoinPrice
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.Portfolio
import com.bitcoinportfolio.domain.repository.PriceRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetPortfolioValueUseCaseTest {

    private val repository: PriceRepository = mockk()
    private val useCase = GetPortfolioValueUseCase(repository)

    @Test
    fun `portfolio value equals amount times price`() = runTest {
        val price = fakeBitcoinPrice(usd = 100_000.0)
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Success(price))

        useCase(0.5).test {
            val item = awaitItem() as DataResult.Success<Portfolio>
            assertEquals(50_000.0, item.data.valueUsd, 0.01)
            assertEquals(0.5, item.data.btcAmount)
            awaitComplete()
        }
    }

    @Test
    fun `isSpoof is always false from this use case`() = runTest {
        val price = fakeBitcoinPrice()
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Success(price))

        useCase(1.0).test {
            val item = awaitItem() as DataResult.Success<Portfolio>
            assertFalse(item.data.isSpoof)
            awaitComplete()
        }
    }

    @Test
    fun `stale flag is propagated to portfolio result`() = runTest {
        val staleTs = 99_999L
        val price = fakeBitcoinPrice(isStale = true)
        every { repository.observeCurrentPrice() } returns
            flowOf(DataResult.Success(price, isStale = true, staleAsOf = staleTs))

        useCase(1.0).test {
            val item = awaitItem() as DataResult.Success<Portfolio>
            assertTrue(item.isStale)
            assertEquals(staleTs, item.staleAsOf)
            awaitComplete()
        }
    }

    @Test
    fun `Loading is passed through`() = runTest {
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Loading)

        useCase(1.0).test {
            assertEquals(DataResult.Loading, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Error is passed through`() = runTest {
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Error("fail"))

        useCase(1.0).test {
            val item = awaitItem() as DataResult.Error
            assertEquals("fail", item.message)
            awaitComplete()
        }
    }

    @Test
    fun `XAU value equals amount times btcInXau`() = runTest {
        val price = fakeBitcoinPrice(usd = 96_000.0, xauPerOz = 3_000.0)
        every { repository.observeCurrentPrice() } returns flowOf(DataResult.Success(price))

        useCase(2.0).test {
            val item = awaitItem() as DataResult.Success<Portfolio>
            // btcInXau = 96_000 / 3_000 = 32; valueXau = 2.0 * 32 = 64
            assertEquals(64.0, item.data.valueXau, 0.001)
            awaitComplete()
        }
    }

    private fun fakeBitcoinPrice(
        usd: Double = 96_000.0,
        xauPerOz: Double = 3_000.0,
        isStale: Boolean = false,
    ) = BitcoinPrice(
        usd = usd,
        xauPerOz = xauPerOz,
        btcInXau = usd / xauPerOz,
        fetchedAtMs = System.currentTimeMillis(),
        isStale = isStale,
    )
}
