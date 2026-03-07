package com.aurachat.domain.repository

import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    // ── Sessions ──────────────────────────────────────────────────────────────

    fun getSessionsFlow(): Flow<List<ChatSession>>

    suspend fun getSessionById(sessionId: Long): ChatSession?

    /** Creates a new session and returns its generated ID for navigation. */
    suspend fun createSession(title: String): Long

    suspend fun deleteSession(sessionId: Long)

    // ── Messages ──────────────────────────────────────────────────────────────

    fun getMessagesFlow(sessionId: Long): Flow<List<ChatMessage>>

    /** Persists a completed message and syncs session metadata. Returns the generated message ID. */
    suspend fun saveMessage(message: ChatMessage): Long

    /** Updates an existing message — used to finalize a streaming response. */
    suspend fun updateMessage(message: ChatMessage)

    suspend fun updateSessionTitle(sessionId: Long, title: String)
}
