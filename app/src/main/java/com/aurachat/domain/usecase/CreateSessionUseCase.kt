package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

class CreateSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Creates a new session and returns its generated ID for immediate navigation. */
    suspend operator fun invoke(initialTitle: String = "New Chat"): Long {
        Timber.d("CreateSessionUseCase invoked with title='$initialTitle'")

        if (initialTitle.isBlank()) {
            Timber.e("Validation failed: empty session title")
            throw DomainError.ValidationError("Session title cannot be empty")
        }

        return try {
            val sessionId = repository.createSession(initialTitle)
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
