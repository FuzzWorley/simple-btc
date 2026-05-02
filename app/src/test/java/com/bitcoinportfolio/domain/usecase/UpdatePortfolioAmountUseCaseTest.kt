package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.data.secure.SecureStorage
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpdatePortfolioAmountUseCaseTest {

    private val secureStorage: SecureStorage = mockk(relaxed = true)
    private val useCase = UpdatePortfolioAmountUseCase(secureStorage)

    @Test
    fun `valid amount returns Success and saves`() {
        val result = useCase.execute(1.5)

        assertEquals(UpdatePortfolioAmountUseCase.Result.Success, result)
        verify { secureStorage.saveBtcAmount(1.5) }
    }

    @Test
    fun `zero returns InvalidAmount and does not save`() {
        val result = useCase.execute(0.0)

        assertEquals(UpdatePortfolioAmountUseCase.Result.InvalidAmount, result)
        verify(exactly = 0) { secureStorage.saveBtcAmount(any()) }
    }

    @Test
    fun `negative amount returns InvalidAmount and does not save`() {
        val result = useCase.execute(-0.5)

        assertEquals(UpdatePortfolioAmountUseCase.Result.InvalidAmount, result)
        verify(exactly = 0) { secureStorage.saveBtcAmount(any()) }
    }

    @Test
    fun `amount with exactly 8 decimal places returns Success`() {
        val result = useCase.execute(0.00000001)

        assertEquals(UpdatePortfolioAmountUseCase.Result.Success, result)
        verify { secureStorage.saveBtcAmount(0.00000001) }
    }

    @Test
    fun `amount with more than 8 decimal places returns InvalidAmount`() {
        val result = useCase.execute(0.000000001)

        assertEquals(UpdatePortfolioAmountUseCase.Result.InvalidAmount, result)
        verify(exactly = 0) { secureStorage.saveBtcAmount(any()) }
    }

    @Test
    fun `large valid amount returns Success`() {
        val result = useCase.execute(21_000_000.0)

        assertEquals(UpdatePortfolioAmountUseCase.Result.Success, result)
        verify { secureStorage.saveBtcAmount(21_000_000.0) }
    }
}
