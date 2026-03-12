package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import com.aurachat.util.Constants
import timber.log.Timber
import javax.inject.Inject

/**
 * Creates a new session whose first prompt is durably stored until the chat screen claims it.
 */
class StartChatSessionUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(prompt: String): Long {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) {
            Timber.e("Validation failed: empty startup prompt")
            throw DomainError.ValidationError("Message cannot be empty")
        }

        return try {
            val sessionId = repository.createSession(
                title = trimmedPrompt.take(Constants.Session.MAX_TITLE_LENGTH),
                pendingInitialPrompt = trimmedPrompt,
            )
            Timber.i("Started chat session id=%d with pending initial prompt", sessionId)
            sessionId
        } catch (e: DomainError) {
            Timber.e(e, "DomainError starting chat session")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error starting chat session")
            throw DomainError.DatabaseError("Failed to start chat: ${e.message}")
        }
    }
}
