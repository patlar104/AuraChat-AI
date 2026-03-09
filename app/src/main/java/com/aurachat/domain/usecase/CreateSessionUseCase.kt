package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import javax.inject.Inject

class CreateSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Creates a new session and returns its generated ID for immediate navigation. */
    suspend operator fun invoke(initialTitle: String = "New Chat"): Long {
        if (initialTitle.isBlank()) {
            throw DomainError.ValidationError("Session title cannot be empty")
        }

        return try {
            repository.createSession(initialTitle)
        } catch (e: DomainError) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DatabaseError("Failed to create session: ${e.message}")
        }
    }
}
