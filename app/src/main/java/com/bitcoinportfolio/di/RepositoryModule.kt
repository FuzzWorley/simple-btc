package com.bitcoinportfolio.di

import com.bitcoinportfolio.data.repository.PortfolioRepositoryImpl
import com.bitcoinportfolio.data.repository.PriceRepositoryImpl
import com.bitcoinportfolio.domain.repository.PortfolioRepository
import com.bitcoinportfolio.domain.repository.PriceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPriceRepository(impl: PriceRepositoryImpl): PriceRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(impl: PortfolioRepositoryImpl): PortfolioRepository
}
