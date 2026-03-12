package com.aurachat.domain.repository

import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

/**
 * Contract for all chat data operations.
 *
 * Implementations ([com.aurachat.data.repository.RoomChatRepository]) are responsible
 * for bridging between the domain layer and the local Room database. All Flow-returning
 * functions emit continuously and should be collected within a lifecycle-aware scope.
 */
interface ChatRepository {

    // ── Sessions ──────────────────────────────────────────────────────────────

    /**
     * Returns a [Flow] that emits all sessions ordered by [ChatSession.updatedAt] descending
     * whenever the session table changes. Does not complete.
     */
    fun getSessionsFlow(): Flow<List<ChatSession>>

    /**
     * Returns the session with the given [sessionId], or null if it doesn't exist.
     */
    suspend fun getSessionById(sessionId: Long): ChatSession?

    /**
     * Creates a new session with the given [title] and returns its generated ID
     * for immediate navigation to the chat screen.
     */
    suspend fun createSession(title: String, pendingInitialPrompt: String? = null): Long

    /**
     * Permanently deletes the session with [sessionId] and all its associated messages
     * (via Room's ForeignKey CASCADE constraint).
     */
    suspend fun deleteSession(sessionId: Long)

    /**
     * Returns the durable startup prompt for [sessionId] and clears it in the same
     * transaction so it can only be auto-sent once.
     */
    suspend fun consumePendingInitialPrompt(sessionId: Long): String?

    // ── Messages ──────────────────────────────────────────────────────────────

    /**
     * Returns a [Flow] that emits the ordered list of messages for [sessionId]
     * whenever the messages table changes. Does not complete.
     */
    fun getMessagesFlow(sessionId: Long): Flow<List<ChatMessage>>

    /**
     * Persists a completed [message] to the database and updates the parent session's
     * [ChatSession.updatedAt], [ChatSession.messageCount], and [ChatSession.lastMessagePreview].
     * Returns the generated message ID.
     */
    suspend fun saveMessage(message: ChatMessage): Long

    /**
     * Updates an existing message in the database — used to finalize or correct a
     * previously saved message.
     */
    suspend fun updateMessage(message: ChatMessage)

    /**
     * Sets the display title of session [sessionId] to [title].
     * Called by [com.aurachat.domain.usecase.UpdateSessionTitleUseCase] after the first
     * exchange to replace the placeholder title with a meaningful summary.
     */
    suspend fun updateSessionTitle(sessionId: Long, title: String)
}
