package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.repository.ChatRepository
import javax.inject.Inject

class SaveMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Persists a completed message and syncs session metadata. Returns the generated message ID. */
    suspend operator fun invoke(message: ChatMessage): Long {
        if (message.content.isBlank()) {
            throw DomainError.ValidationError("Message content cannot be empty")
        }

        return try {
            repository.saveMessage(message)
        } catch (e: DomainError) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DatabaseError("Failed to save message: ${e.message}")
        }
    }
}
