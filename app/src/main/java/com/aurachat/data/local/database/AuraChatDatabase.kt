package com.aurachat.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aurachat.data.local.dao.ChatMessageDao
import com.aurachat.data.local.dao.ChatSessionDao
import com.aurachat.data.local.entity.ChatMessageEntity
import com.aurachat.data.local.entity.ChatSessionEntity

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AuraChatDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DATABASE_NAME = "aura_chat.db"
    }
}
