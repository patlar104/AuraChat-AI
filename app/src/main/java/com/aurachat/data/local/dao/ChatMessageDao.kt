package com.aurachat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aurachat.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [ChatMessageEntity] CRUD operations.
 *
 * All suspend functions run on the Room executor and are safe to call from any
 * coroutine dispatcher. The [Flow]-returning function emits continuously and should
 * be collected within a lifecycle-aware scope.
 */
@Dao
interface ChatMessageDao {

    /**
     * Returns a [Flow] that emits messages for [sessionId] ordered by timestamp ascending
     * (oldest first) whenever the table changes.
     */
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: Long): Flow<List<ChatMessageEntity>>

    /**
     * Inserts [message] and returns the generated row ID (the new message's `id`).
     * Aborts on conflict (should never occur with auto-generated PKs).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    /** Batch-inserts [messages]. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    /** Replaces an existing message row with the updated [message] data. */
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    /** Deletes the given [message] entity by its primary key. */
    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    /** Returns the total number of messages in the session identified by [sessionId]. */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE session_id = :sessionId")
    suspend fun countMessages(sessionId: Long): Int

    /**
     * Returns the most recently created message in [sessionId], or null if the session
     * has no messages.
     */
    @Query("""
        SELECT * FROM chat_messages
        WHERE session_id = :sessionId
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getLatestMessage(sessionId: Long): ChatMessageEntity?
}
