package com.xxh.ringbones.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds repository interfaces to their data-layer implementations.
 * Populated in Tasks 11-13 after data + domain layers exist.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule
