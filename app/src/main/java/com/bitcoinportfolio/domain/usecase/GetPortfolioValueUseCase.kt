package com.bitcoinportfolio.domain.usecase

import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.Portfolio
import com.bitcoinportfolio.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPortfolioValueUseCase @Inject constructor(
    private val priceRepository: PriceRepository,
) {
    operator fun invoke(btcAmount: Double): Flow<DataResult<Portfolio>> =
        priceRepository.observeCurrentPrice().map { result ->
            when (result) {
                is DataResult.Success -> DataResult.Success(
                    Portfolio(btcAmount, result.data, isSpoof = false),
                    result.isStale,
                    result.staleAsOf,
                )
                is DataResult.Error -> DataResult.Error(result.message)
                DataResult.Loading -> DataResult.Loading
            }
        }
}
