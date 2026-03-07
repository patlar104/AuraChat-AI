package com.aurachat.domain.usecase

import com.aurachat.domain.repository.ChatRepository
import javax.inject.Inject

class CreateSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Creates a new session and returns its generated ID for immediate navigation. */
    suspend operator fun invoke(initialTitle: String = "New Chat"): Long =
        repository.createSession(initialTitle)
}
