package com.xxh.ringbones.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Timeout duration for OkHttp connections and reads, in seconds. */
private const val HTTP_TIMEOUT_SECONDS = 30L

/**
 * Provides application-scoped singleton dependencies: OkHttp network client.
 * Additional providers (Database, DataStore) are added in later tasks.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Creates a singleton OkHttpClient with 30-second connect and read timeouts.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
}
