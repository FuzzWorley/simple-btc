package com.bitcoinportfolio.di

import com.bitcoinportfolio.data.secure.SecureStorage
import com.bitcoinportfolio.data.secure.SecureStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindSecureStorage(impl: SecureStorageImpl): SecureStorage
}
