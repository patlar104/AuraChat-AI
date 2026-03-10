package com.aurachat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aurachat.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [ChatSessionEntity] CRUD operations.
 *
 * All suspend functions run on the Room executor and are safe to call from any
 * coroutine dispatcher. The [Flow]-returning function emits continuously and should
 * be collected within a lifecycle-aware scope.
 */
@Dao
interface ChatSessionDao {

    /**
     * Returns a [Flow] that emits all sessions ordered by `updated_at DESC`
     * (newest conversation first) whenever the table changes.
     */
    @Query("SELECT * FROM chat_sessions ORDER BY updated_at DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSessionEntity>>

    /**
     * Returns the session with the given [sessionId], or null if not found.
     */
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChatSessionEntity?

    /**
     * Inserts [session] and returns the generated row ID (the new session's `id`).
     * Aborts on conflict (should never occur with auto-generated PKs).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: ChatSessionEntity): Long

    /** Replaces an existing session row with the updated [session] data. */
    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    /** Deletes the given [session] entity by its primary key. */
    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    /** Deletes the session identified by [sessionId] directly by primary key. */
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}
