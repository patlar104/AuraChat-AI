package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long) {
        try {
            repository.deleteSession(sessionId)
        } catch (e: DomainError) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DatabaseError("Failed to delete session: ${e.message}")
        }
    }
}
