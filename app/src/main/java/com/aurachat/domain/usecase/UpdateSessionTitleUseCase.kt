package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import javax.inject.Inject

class UpdateSessionTitleUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long, title: String) {
        if (title.isBlank()) {
            throw DomainError.ValidationError("Session title cannot be empty")
        }

        try {
            repository.updateSessionTitle(sessionId, title)
        } catch (e: DomainError) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DatabaseError("Failed to update session title: ${e.message}")
        }
    }
}
