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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuraChatDatabase =
        Room.databaseBuilder(
            context,
            AuraChatDatabase::class.java,
            AuraChatDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Provides
    fun provideChatSessionDao(db: AuraChatDatabase): ChatSessionDao = db.chatSessionDao()

    @Provides
    fun provideChatMessageDao(db: AuraChatDatabase): ChatMessageDao = db.chatMessageDao()
}

// @Binds requires an abstract function, so it lives in a separate abstract module
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: RoomChatRepository): ChatRepository
}
