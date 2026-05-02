package com.bitcoinportfolio.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_price")
data class CurrentPriceEntity(
    @PrimaryKey val id: Int = 1,
    val btcUsd: Double,
    val xauUsd: Double,
    val btcXau: Double,
    val fetchedAtMs: Long
)
