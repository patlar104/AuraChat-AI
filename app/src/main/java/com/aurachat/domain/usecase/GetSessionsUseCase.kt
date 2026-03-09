package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class GetSessionsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatSession>> =
        repository.getSessionsFlow()
            .catch { e ->
                throw when (e) {
                    is DomainError -> e
                    else -> DomainError.DatabaseError("Failed to get sessions: ${e.message}")
                }
            }
}
