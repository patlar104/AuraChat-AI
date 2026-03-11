package com.aurachat.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.aurachat.data.settings.DataStoreSettingsRepository
import com.aurachat.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "settings")

/**
 * Hilt module that provides the Preferences DataStore for app settings.
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore
}

/**
 * Hilt module that binds [DataStoreSettingsRepository] as the [SettingsRepository] implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindingModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: DataStoreSettingsRepository,
    ): SettingsRepository
}
