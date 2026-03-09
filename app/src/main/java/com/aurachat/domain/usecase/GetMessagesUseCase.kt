package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(sessionId: Long): Flow<List<ChatMessage>> =
        repository.getMessagesFlow(sessionId)
            .catch { e ->
                throw when (e) {
                    is DomainError -> e
                    else -> DomainError.DatabaseError("Failed to get messages: ${e.message}")
                }
            }
}
