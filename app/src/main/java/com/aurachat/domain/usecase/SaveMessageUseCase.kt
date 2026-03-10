package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case that persists a chat message and updates session metadata.
 *
 * Validates that the message content is not blank and delegates to the repository
 * for persistence. The repository also updates the session's updatedAt timestamp
 * and preview text to keep the session list in sync.
 */
class SaveMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Persists a chat message and syncs session metadata.
     *
     * @param message The message to save (USER or MODEL role)
     * @return The ID of the saved message
     * @throws DomainError.ValidationError if the message content is blank
     * @throws DomainError.DatabaseError if the operation fails
     */
    suspend operator fun invoke(message: ChatMessage): Long {
        Timber.d("SaveMessageUseCase invoked for sessionId=${message.sessionId}, role=${message.role}")

        if (message.content.isBlank()) {
            Timber.e("Validation failed: empty message content")
            throw DomainError.ValidationError("Message content cannot be empty")
        }

        return try {
            val messageId = repository.saveMessage(message)
            Timber.d("Message saved with id=$messageId")
            messageId
        } catch (e: DomainError) {
            Timber.e(e, "DomainError saving message")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error saving message")
            throw DomainError.DatabaseError("Failed to save message: ${e.message}")
        }
    }
}
