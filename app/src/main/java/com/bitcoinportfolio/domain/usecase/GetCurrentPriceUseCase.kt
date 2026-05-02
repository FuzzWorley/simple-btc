package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.domain.model.BitcoinPrice
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentPriceUseCase @Inject constructor(
    private val priceRepository: PriceRepository,
) {
    operator fun invoke(): Flow<DataResult<BitcoinPrice>> = priceRepository.observeCurrentPrice()
}
