package com.aurachat.data.repository

import com.aurachat.data.local.dao.ChatMessageDao
import com.aurachat.data.local.dao.ChatSessionDao
import com.aurachat.data.local.database.AuraChatDatabase
import com.aurachat.data.local.entity.ChatSessionEntity
import com.aurachat.data.local.entity.toDomain
import com.aurachat.data.local.entity.toEntity
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.repository.ChatRepository
import com.aurachat.util.Constants
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [ChatRepository].
 *
 * All operations map between the domain model layer and Room entities. Session metadata
 * (preview, message count, updatedAt) is denormalized into [ChatSessionEntity] and kept
 * in sync on every [saveMessage] call to avoid expensive JOIN queries in the history list.
 */
@Singleton
class RoomChatRepository @Inject constructor(
    private val database: AuraChatDatabase,
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao,
) : ChatRepository {

    override fun getSessionsFlow(): Flow<List<ChatSession>> =
        sessionDao.getAllSessionsFlow().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSessionById(sessionId: Long): ChatSession? =
        sessionDao.getSessionById(sessionId)?.toDomain()

    override suspend fun createSession(title: String, pendingInitialPrompt: String?): Long {
        val now = System.currentTimeMillis()
        val id = sessionDao.insertSession(
            ChatSessionEntity(
                title = title,
                createdAt = now,
                updatedAt = now,
                pendingInitialPrompt = pendingInitialPrompt,
            ),
        )
        Timber.d("Created session id=%d title=%s", id, title)
        return id
    }

    override suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSessionById(sessionId)
        Timber.d("Deleted session id=%d (messages cascade-deleted by Room)", sessionId)
    }

    override suspend fun consumePendingInitialPrompt(sessionId: Long): String? =
        database.withTransaction {
            val session = sessionDao.getSessionById(sessionId) ?: return@withTransaction null
            val prompt = session.pendingInitialPrompt?.takeIf { it.isNotBlank() } ?: return@withTransaction null
            sessionDao.updateSession(session.copy(pendingInitialPrompt = null))
            Timber.d("Consumed pending initial prompt for session id=%d", sessionId)
            prompt
        }

    override fun getMessagesFlow(sessionId: Long): Flow<List<ChatMessage>> =
        messageDao.getMessagesForSessionFlow(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveMessage(message: ChatMessage): Long {
        val messageId = messageDao.insertMessage(message.toEntity())
        // Sync denormalized session metadata so the drawer list stays up-to-date
        val session = sessionDao.getSessionById(message.sessionId)
        if (session != null) {
            sessionDao.updateSession(
                session.copy(
                    updatedAt = message.timestamp,
                    messageCount = messageDao.countMessages(message.sessionId),
                    lastMessagePreview = message.content.take(Constants.Session.MAX_PREVIEW_LENGTH),
                ),
            )
        }
        Timber.d("Saved message id=%d sessionId=%d role=%s", messageId, message.sessionId, message.role)
        return messageId
    }

    override suspend fun updateMessage(message: ChatMessage) {
        messageDao.updateMessage(message.toEntity())
        Timber.d("Updated message id=%d", message.id)
    }

    override suspend fun updateSessionTitle(sessionId: Long, title: String) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        sessionDao.updateSession(session.copy(title = title.take(Constants.Session.MAX_TITLE_LENGTH)))
        Timber.d("Updated session title id=%d title=%s", sessionId, title)
    }
}
