package com.bitcoinportfolio.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bitcoinportfolio.data.db.entity.CurrentPriceEntity
import com.bitcoinportfolio.data.db.entity.PriceHistoryEntity

@Database(
    entities = [CurrentPriceEntity::class, PriceHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun priceDao(): PriceDao
}
