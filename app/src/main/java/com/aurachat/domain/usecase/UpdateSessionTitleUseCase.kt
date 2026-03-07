package com.aurachat.domain.usecase

import com.aurachat.domain.repository.ChatRepository
import javax.inject.Inject

class UpdateSessionTitleUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(sessionId: Long, title: String) =
        repository.updateSessionTitle(sessionId, title)
}
