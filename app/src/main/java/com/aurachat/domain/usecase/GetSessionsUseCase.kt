package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case that retrieves all chat sessions as a reactive Flow.
 *
 * Observes the session list from the repository and emits updates whenever sessions
 * are created, modified, or deleted. Sessions are typically sorted by most recent first.
 * Provides error handling and logging for database operations.
 */
class GetSessionsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Retrieves all chat sessions as a reactive Flow.
     *
     * @return Flow that emits the list of chat sessions sorted by most recent
     * @throws DomainError.DatabaseError if the operation fails
     */
    operator fun invoke(): Flow<List<ChatSession>> =
        repository.getSessionsFlow()
            .onStart { Timber.d("GetSessionsUseCase invoked") }
            .catch { e ->
                Timber.e(e, "Error getting sessions")
                throw when (e) {
                    is DomainError -> e
                    else -> DomainError.DatabaseError("Failed to get sessions: ${e.message}")
                }
            }
}
