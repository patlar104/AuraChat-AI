package com.aurachat.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aurachat.data.local.dao.ChatMessageDao
import com.aurachat.data.local.dao.ChatSessionDao
import com.aurachat.data.local.entity.ChatMessageEntity
import com.aurachat.data.local.entity.ChatSessionEntity

/**
 * The single Room database instance for AuraChat.
 *
 * Accessed exclusively through Hilt-provided DAOs ([ChatSessionDao], [ChatMessageDao]).
 * Schema version 3. Destructive migration is enabled for development builds — bump
 * [version] and add a proper migration when the schema changes in production.
 *
 * Constructed by [com.aurachat.di.DatabaseModule].
 */
@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AuraChatDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        /** The on-disk filename for the Room database. */
        const val DATABASE_NAME = "aura_chat.db"
    }
}
