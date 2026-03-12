package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case that creates a new chat session with an initial title.
 *
 * Validates the title is not blank and delegates to the repository for persistence.
 * Returns the generated session ID for immediate navigation to the new chat.
 */
class CreateSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Creates a new chat session and returns its generated ID.
     *
     * @param initialTitle The initial title for the session (default: "New Chat")
     * @return The ID of the newly created session
     * @throws DomainError.ValidationError if the title is blank
     * @throws DomainError.DatabaseError if the operation fails
     */
    suspend operator fun invoke(initialTitle: String = "New Chat"): Long {
        Timber.d("CreateSessionUseCase invoked with title='$initialTitle'")

        if (initialTitle.isBlank()) {
            Timber.e("Validation failed: empty session title")
            throw DomainError.ValidationError("Session title cannot be empty")
        }

        return try {
            val sessionId = repository.createSession(initialTitle, pendingInitialPrompt = null)
            Timber.i("Session created with id=$sessionId")
            sessionId
        } catch (e: DomainError) {
            Timber.e(e, "DomainError creating session")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error creating session")
            throw DomainError.DatabaseError("Failed to create session: ${e.message}")
        }
    }
}
