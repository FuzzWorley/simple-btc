package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.PricePoint
import com.bitcoinportfolio.domain.model.TimeRange
import com.bitcoinportfolio.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPriceHistoryUseCase @Inject constructor(
    private val priceRepository: PriceRepository,
) {
    operator fun invoke(timeRange: TimeRange, currency: Currency): Flow<DataResult<List<PricePoint>>> =
        priceRepository.observePriceHistory(timeRange, currency)
}
