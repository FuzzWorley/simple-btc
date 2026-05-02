package com.bitcoinportfolio.domain.repository

import com.bitcoinportfolio.domain.model.BitcoinPrice
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.PricePoint
import com.bitcoinportfolio.domain.model.TimeRange
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    fun observeCurrentPrice(): Flow<DataResult<BitcoinPrice>>
    fun observePriceHistory(timeRange: TimeRange, currency: Currency): Flow<DataResult<List<PricePoint>>>
    suspend fun refreshCurrentPrice()
    suspend fun refreshHistory(timeRange: TimeRange)
}
