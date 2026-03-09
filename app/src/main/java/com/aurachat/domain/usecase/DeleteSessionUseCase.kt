package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case that deletes a chat session and all its associated messages.
 *
 * Delegates to the repository to handle cascade deletion of messages and session metadata.
 * Provides error handling and logging for database operations.
 */
class DeleteSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Deletes the specified chat session and all its messages.
     *
     * @param sessionId The ID of the session to delete
     * @throws DomainError.DatabaseError if the operation fails
     */
    suspend operator fun invoke(sessionId: Long) {
        Timber.d("DeleteSessionUseCase invoked for sessionId=$sessionId")

        try {
            repository.deleteSession(sessionId)
            Timber.i("Session deleted: sessionId=$sessionId")
        } catch (e: DomainError) {
            Timber.e(e, "DomainError deleting session $sessionId")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error deleting session $sessionId")
            throw DomainError.DatabaseError("Failed to delete session: ${e.message}")
        }
    }
}
