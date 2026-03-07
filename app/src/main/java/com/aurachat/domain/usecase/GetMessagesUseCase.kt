package com.aurachat.domain.usecase

import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(sessionId: Long): Flow<List<ChatMessage>> =
        repository.getMessagesFlow(sessionId)
}
