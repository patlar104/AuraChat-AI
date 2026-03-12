package com.aurachat.di

import com.aurachat.data.local.database.AuraChatDatabase
import com.aurachat.domain.repository.ChatRepository
import com.aurachat.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppGraphEntryPoint {
    fun database(): AuraChatDatabase
    fun chatRepository(): ChatRepository
    fun settingsRepository(): SettingsRepository
}
