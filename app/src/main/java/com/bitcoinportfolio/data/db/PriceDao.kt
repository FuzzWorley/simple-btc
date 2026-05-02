package com.bitcoinportfolio.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitcoinportfolio.data.db.entity.CurrentPriceEntity
import com.bitcoinportfolio.data.db.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceDao {

    @Query("SELECT * FROM current_price WHERE id = 1")
    fun observeCurrentPrice(): Flow<CurrentPriceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrentPrice(entity: CurrentPriceEntity)

    @Query("SELECT * FROM price_history WHERE symbol = :symbol AND timeRange = :timeRange ORDER BY timestampMs ASC")
    fun observeHistory(symbol: String, timeRange: String): Flow<List<PriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entities: List<PriceHistoryEntity>)

    @Query("DELETE FROM price_history WHERE symbol = :symbol AND timeRange = :timeRange")
    suspend fun clearHistory(symbol: String, timeRange: String)

    @Query("SELECT MAX(fetchedAtMs) FROM price_history WHERE symbol = :symbol AND timeRange = :timeRange")
    suspend fun getLastFetchedAt(symbol: String, timeRange: String): Long?
}
