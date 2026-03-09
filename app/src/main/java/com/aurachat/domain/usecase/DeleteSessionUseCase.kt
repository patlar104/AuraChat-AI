package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

class DeleteSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long) {
        Timber.d("DeleteSessionUseCase invoked for sessionId=$sessionId")

        try {
            repository.deleteSession(sessionId)
            Timber.i("Session deleted: sessionId=$sessionId")
        } catch (e: DomainError) {
            Timber.e(e, "DomainError deleting session $sessionId")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error deleting session $sessionId")
            throw DomainError.DatabaseError("Failed to delete session: ${e.message}")
        }
    }
}
