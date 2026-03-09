package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case that updates the title of an existing chat session.
 *
 * Validates that the title is not blank and delegates to the repository for persistence.
 * Used for manual title edits and automatic titling from the first user message.
 */
class UpdateSessionTitleUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Updates the title of the specified chat session.
     *
     * @param sessionId The ID of the session to update
     * @param title The new title for the session
     * @throws DomainError.ValidationError if the title is blank
     * @throws DomainError.DatabaseError if the operation fails
     */
    suspend operator fun invoke(sessionId: Long, title: String) {
        Timber.d("UpdateSessionTitleUseCase invoked for sessionId=$sessionId, title='$title'")

        if (title.isBlank()) {
            Timber.e("Validation failed: empty session title")
            throw DomainError.ValidationError("Session title cannot be empty")
        }

        try {
            repository.updateSessionTitle(sessionId, title)
            Timber.d("Session title updated for sessionId=$sessionId")
        } catch (e: DomainError) {
            Timber.e(e, "DomainError updating session title for sessionId=$sessionId")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error updating session title for sessionId=$sessionId")
            throw DomainError.DatabaseError("Failed to update session title: ${e.message}")
        }
    }
}
