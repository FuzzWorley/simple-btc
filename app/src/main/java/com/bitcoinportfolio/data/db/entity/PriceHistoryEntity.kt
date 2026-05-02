package com.bitcoinportfolio.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_history",
    indices = [Index(value = ["symbol", "timeRange", "timestampMs"])]
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val timeRange: String,
    val timestampMs: Long,
    val price: Double,
    val fetchedAtMs: Long
)
