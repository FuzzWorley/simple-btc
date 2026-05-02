package com.bitcoinportfolio.domain.usecase

import app.cash.turbine.test
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.PricePoint
import com.bitcoinportfolio.domain.model.TimeRange
import com.bitcoinportfolio.domain.repository.PriceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetPriceHistoryUseCaseTest {

    private val repository: PriceRepository = mockk()
    private val useCase = GetPriceHistoryUseCase(repository)

    @Test
    fun `invoke delegates to repository with correct args`() = runTest {
        val points = listOf(PricePoint(1_000L, 95_000.0), PricePoint(2_000L, 96_000.0))
        every { repository.observePriceHistory(TimeRange.ONE_MONTH, Currency.USD) } returns
            flowOf(DataResult.Success(points))

        useCase(TimeRange.ONE_MONTH, Currency.USD).test {
            val item = awaitItem() as DataResult.Success
            assertEquals(2, item.data.size)
            awaitComplete()
        }

        verify(exactly = 1) { repository.observePriceHistory(TimeRange.ONE_MONTH, Currency.USD) }
    }

    @Test
    fun `invoke passes XAU currency to repository`() = runTest {
        val xauPoints = listOf(PricePoint(1_000L, 32.0))
        every { repository.observePriceHistory(TimeRange.ONE_WEEK, Currency.XAU) } returns
            flowOf(DataResult.Success(xauPoints))

        useCase(TimeRange.ONE_WEEK, Currency.XAU).test {
            val item = awaitItem() as DataResult.Success
            assertEquals(32.0, item.data[0].priceUsd, 0.001)
            awaitComplete()
        }
    }

    @Test
    fun `invoke passes through Loading`() = runTest {
        every { repository.observePriceHistory(any(), any()) } returns flowOf(DataResult.Loading)

        useCase(TimeRange.ONE_MONTH, Currency.USD).test {
            assertEquals(DataResult.Loading, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `invoke passes through Error`() = runTest {
        every { repository.observePriceHistory(any(), any()) } returns
            flowOf(DataResult.Error("api failure"))

        useCase(TimeRange.ONE_MONTH, Currency.USD).test {
            val item = awaitItem() as DataResult.Error
            assertEquals("api failure", item.message)
            awaitComplete()
        }
    }
}
