package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

class SaveMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Persists a completed message and syncs session metadata. Returns the generated message ID. */
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
