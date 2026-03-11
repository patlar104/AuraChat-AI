package com.aurachat.di

import com.aurachat.data.remote.GeminiDataSource
import com.aurachat.data.remote.GeminiDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [GeminiDataSourceImpl] as the [GeminiDataSource] implementation.
 *
 * The [GenerativeModel] is no longer a singleton here — [GeminiDataSourceImpl] creates
 * a new model instance per request using the model name from [SettingsRepository], so
 * that switching models in Settings takes effect immediately on the next send.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GeminiBindingModule {

    @Binds
    @Singleton
    abstract fun bindGeminiDataSource(impl: GeminiDataSourceImpl): GeminiDataSource
}
