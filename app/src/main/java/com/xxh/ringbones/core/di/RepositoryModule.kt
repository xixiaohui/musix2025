package com.xxh.ringbones.core.di

import com.xxh.ringbones.data.repository.FavoriteRepositoryImpl
import com.xxh.ringbones.data.repository.PlayHistoryRepositoryImpl
import com.xxh.ringbones.data.repository.RingtoneRepositoryImpl
import com.xxh.ringbones.data.repository.SearchHistoryRepositoryImpl
import com.xxh.ringbones.domain.repository.FavoriteRepository
import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import com.xxh.ringbones.domain.repository.RingtoneRepository
import com.xxh.ringbones.domain.repository.SearchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds repository interfaces to their data-layer implementations.
 * Domain interfaces are defined in the domain layer (Tasks 11-12).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRingtoneRepository(impl: RingtoneRepositoryImpl): RingtoneRepository

    @Binds
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    abstract fun bindPlayHistoryRepository(impl: PlayHistoryRepositoryImpl): PlayHistoryRepository

    @Binds
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository
}
