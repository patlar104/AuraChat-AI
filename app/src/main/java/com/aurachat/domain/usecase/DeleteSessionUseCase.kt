package com.aurachat.domain.usecase

import com.aurachat.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long) = repository.deleteSession(sessionId)
}
