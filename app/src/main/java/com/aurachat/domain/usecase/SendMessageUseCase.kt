package com.aurachat.domain.usecase

import com.aurachat.data.remote.GeminiDataSource
import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
    private val geminiDataSource: GeminiDataSource,
    private val updateSessionTitle: UpdateSessionTitleUseCase,
) {
    /**
     * Orchestrates a full send/receive cycle:
     * 1. Snapshots history before saving (to detect first-ever message for auto-titling)
     * 2. Saves the user message to Room
     * 3. Streams Gemini's response — each chunk is emitted for the UI to display live
     * 4. Saves the completed AI response to Room
     * 5. Auto-titles the session from the first user message (first exchange only)
     *
     * Returns Flow<String> — the ViewModel collects chunks and appends them to a
     * MutableStateFlow<String> to drive the streaming bubble in the chat UI (Phase 5).
     */
    operator fun invoke(sessionId: Long, userPrompt: String): Flow<String> = flow {
        // Validate input
        if (userPrompt.isBlank()) {
            throw DomainError.ValidationError("Message cannot be empty")
        }

        val now = System.currentTimeMillis()

        try {
            // Snapshot BEFORE saving so we can detect the first message and pass
            // correct prior history to Gemini (excludes the current user prompt)
            val historyBefore = repository.getMessagesFlow(sessionId).first()
            val isFirstMessage = historyBefore.isEmpty()

            // Persist user message — also syncs session updatedAt + preview via repository
            repository.saveMessage(
                ChatMessage(
                    sessionId = sessionId,
                    content = userPrompt,
                    role = MessageRole.USER,
                    timestamp = now,
                )
            )

            // Stream from Gemini, re-emitting each chunk to the collector (ViewModel)
            val fullResponse = StringBuilder()
            geminiDataSource.sendMessage(historyBefore, userPrompt)
                .catch { e ->
                    throw when (e) {
                        is IOException -> DomainError.NetworkError("Failed to connect to AI service: ${e.message}")
                        is DomainError -> e
                        else -> DomainError.ApiError(0, "AI service error: ${e.message}")
                    }
                }
                .collect { chunk ->
                    fullResponse.append(chunk)
                    emit(chunk)
                }

            // Persist the completed AI response
            repository.saveMessage(
                ChatMessage(
                    sessionId = sessionId,
                    content = fullResponse.toString(),
                    role = MessageRole.MODEL,
                    timestamp = System.currentTimeMillis(),
                )
            )

            // Auto-title the session from the user's first message (truncated to 60 chars)
            if (isFirstMessage) {
                updateSessionTitle(sessionId, userPrompt.take(60))
            }
        } catch (e: DomainError) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DatabaseError("Failed to send message: ${e.message}")
        }
    }
}
