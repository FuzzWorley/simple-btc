package com.bitcoinportfolio.di

import com.bitcoinportfolio.BuildConfig
import com.bitcoinportfolio.data.api.CoinGeckoService
import com.bitcoinportfolio.data.api.MetalsLiveService
import com.bitcoinportfolio.data.api.StooqService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("coingecko")
    fun provideCoinGeckoRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("metalslive")
    fun provideMetalsLiveRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://metals.live/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    // No converter needed: StooqService returns ResponseBody directly
    @Provides
    @Singleton
    @Named("stooq")
    fun provideStooqRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://stooq.com/")
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    fun provideCoinGeckoService(@Named("coingecko") retrofit: Retrofit): CoinGeckoService =
        retrofit.create(CoinGeckoService::class.java)

    @Provides
    @Singleton
    fun provideMetalsLiveService(@Named("metalslive") retrofit: Retrofit): MetalsLiveService =
        retrofit.create(MetalsLiveService::class.java)

    @Provides
    @Singleton
    fun provideStooqService(@Named("stooq") retrofit: Retrofit): StooqService =
        retrofit.create(StooqService::class.java)
}
