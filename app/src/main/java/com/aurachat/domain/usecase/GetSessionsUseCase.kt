package com.aurachat.domain.usecase

import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatSession>> = repository.getSessionsFlow()
}
