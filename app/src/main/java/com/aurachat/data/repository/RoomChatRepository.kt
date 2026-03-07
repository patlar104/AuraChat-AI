package com.aurachat.data.repository

import com.aurachat.data.local.dao.ChatMessageDao
import com.aurachat.data.local.dao.ChatSessionDao
import com.aurachat.data.local.entity.ChatSessionEntity
import com.aurachat.data.local.entity.toDomain
import com.aurachat.data.local.entity.toEntity
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomChatRepository @Inject constructor(
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao
) : ChatRepository {

    override fun getSessionsFlow(): Flow<List<ChatSession>> =
        sessionDao.getAllSessionsFlow().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSessionById(sessionId: Long): ChatSession? =
        sessionDao.getSessionById(sessionId)?.toDomain()

    override suspend fun createSession(title: String): Long {
        val now = System.currentTimeMillis()
        return sessionDao.insertSession(
            ChatSessionEntity(title = title, createdAt = now, updatedAt = now)
        )
    }

    override suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSessionById(sessionId)
        // ForeignKey.CASCADE handles deleting child messages automatically
    }

    override fun getMessagesFlow(sessionId: Long): Flow<List<ChatMessage>> =
        messageDao.getMessagesForSessionFlow(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveMessage(message: ChatMessage): Long {
        val messageId = messageDao.insertMessage(message.toEntity())
        val session = sessionDao.getSessionById(message.sessionId)
        if (session != null) {
            sessionDao.updateSession(
                session.copy(
                    updatedAt = message.timestamp,
                    messageCount = messageDao.countMessages(message.sessionId),
                    lastMessagePreview = message.content.take(80)
                )
            )
        }
        return messageId
    }

    override suspend fun updateMessage(message: ChatMessage) {
        messageDao.updateMessage(message.toEntity())
    }

    override suspend fun updateSessionTitle(sessionId: Long, title: String) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        sessionDao.updateSession(session.copy(title = title.take(60)))
    }
}
