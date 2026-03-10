package com.aurachat.di

import android.content.Context
import androidx.room.Room
import com.aurachat.data.local.dao.ChatMessageDao
import com.aurachat.data.local.dao.ChatSessionDao
import com.aurachat.data.local.database.AuraChatDatabase
import com.aurachat.data.repository.RoomChatRepository
import com.aurachat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and its DAOs.
 *
 * Destructive migration is enabled for development — replace with a proper
 * [androidx.room.migration.Migration] before shipping to production.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Provides the singleton [AuraChatDatabase] instance built from the application context. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuraChatDatabase =
        Room.databaseBuilder(
            context,
            AuraChatDatabase::class.java,
            AuraChatDatabase.DATABASE_NAME,
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    /** Provides the [ChatSessionDao] sourced from the singleton database. */
    @Provides
    fun provideChatSessionDao(db: AuraChatDatabase): ChatSessionDao = db.chatSessionDao()

    /** Provides the [ChatMessageDao] sourced from the singleton database. */
    @Provides
    fun provideChatMessageDao(db: AuraChatDatabase): ChatMessageDao = db.chatMessageDao()
}

/**
 * Hilt module that binds [RoomChatRepository] as the [ChatRepository] implementation.
 *
 * [@Binds] requires an abstract function, which is why this lives in a separate
 * abstract module from [DatabaseModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: RoomChatRepository): ChatRepository
}
