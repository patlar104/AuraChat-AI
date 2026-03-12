package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Claims and clears a session's pending startup prompt so it can only auto-send once.
 */
class ConsumePendingInitialPromptUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(sessionId: Long): String? =
        try {
            repository.consumePendingInitialPrompt(sessionId)
        } catch (e: DomainError) {
            Timber.e(e, "DomainError consuming pending initial prompt")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error consuming pending initial prompt")
            throw DomainError.DatabaseError("Failed to load startup prompt: ${e.message}")
        }
}
