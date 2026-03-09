package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case that retrieves messages for a specific chat session as a reactive Flow.
 *
 * Observes the message list from the repository and emits updates whenever messages
 * are added, modified, or deleted. Provides error handling and logging for database operations.
 */
class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Retrieves all messages for the specified session as a reactive Flow.
     *
     * @param sessionId The ID of the chat session to retrieve messages for
     * @return Flow that emits the list of messages sorted by timestamp
     * @throws DomainError.DatabaseError if the operation fails
     */
    operator fun invoke(sessionId: Long): Flow<List<ChatMessage>> =
        repository.getMessagesFlow(sessionId)
            .onStart { Timber.d("GetMessagesUseCase invoked for sessionId=$sessionId") }
            .catch { e ->
                Timber.e(e, "Error getting messages for sessionId=$sessionId")
                throw when (e) {
                    is DomainError -> e
                    else -> DomainError.DatabaseError("Failed to get messages: ${e.message}")
                }
            }
}
