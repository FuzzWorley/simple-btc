package com.bitcoinportfolio.di

import android.content.Context
import androidx.room.Room
import com.bitcoinportfolio.data.db.AppDatabase
import com.bitcoinportfolio.data.db.PriceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bitcoin_portfolio.db"
        ).build()

    @Provides
    @Singleton
    fun providePriceDao(database: AppDatabase): PriceDao = database.priceDao()
}
