package com.aurachat.domain.usecase

import android.graphics.Bitmap
import com.aurachat.data.remote.GeminiDataSource
import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
    private val geminiDataSource: GeminiDataSource,
    private val updateSessionTitle: UpdateSessionTitleUseCase,
) {
    operator fun invoke(
        sessionId: Long,
        userPrompt: String,
    ): Flow<String> = invoke(
        sessionId = sessionId,
        userPrompt = userPrompt,
        imageBitmap = null,
        imageUri = null,
    )

    operator fun invoke(
        sessionId: Long,
        userPrompt: String,
        imageBitmap: Bitmap?,
    ): Flow<String> = invoke(
        sessionId = sessionId,
        userPrompt = userPrompt,
        imageBitmap = imageBitmap,
        imageUri = null,
    )

    /**
     * Orchestrates a full send/receive cycle:
     * 1. Snapshots history before saving (to detect first-ever message for auto-titling)
     * 2. Saves the user message to Room
     * 3. Streams Gemini's response — each chunk is emitted for the UI to display live
     * 4. Saves the completed AI response to Room
     * 5. Auto-titles the session from the first user message (first exchange only)
     *
     * [imageBitmap] is optional. When provided, it is sent to Gemini as an inline
     * image for vision-based queries (Gemini Vision API).
     *
     * Returns Flow<String> — the ViewModel collects chunks and appends them to a
     * MutableStateFlow<String> to drive the streaming bubble in the chat UI.
     */
    operator fun invoke(
        sessionId: Long,
        userPrompt: String,
        imageBitmap: Bitmap? = null,
        imageUri: String? = null,
    ): Flow<String> = flow {
        Timber.d("SendMessageUseCase invoked for sessionId=$sessionId, hasImage=${imageBitmap != null}, prompt=${userPrompt.take(30)}...")

        // Validate input
        if (userPrompt.isBlank()) {
            Timber.e("Validation failed: empty message")
            throw DomainError.ValidationError("Message cannot be empty")
        }

        val now = System.currentTimeMillis()

        try {
            // Snapshot BEFORE saving so we can detect the first message and pass
            // correct prior history to Gemini (excludes the current user prompt)
            val historyBefore = repository.getMessagesFlow(sessionId).first()
            val isFirstMessage = historyBefore.isEmpty()
            Timber.d("History snapshot: ${historyBefore.size} messages, isFirstMessage=$isFirstMessage")

            // Persist user message — also syncs session updatedAt + preview via repository
            repository.saveMessage(
                ChatMessage(
                    sessionId = sessionId,
                    content = userPrompt,
                    imageUri = imageUri,
                    role = MessageRole.USER,
                    timestamp = now,
                )
            )
            Timber.d("User message saved to repository")

            // Stream from Gemini, re-emitting each chunk to the collector (ViewModel)
            Timber.d("Starting Gemini streaming request")
            val fullResponse = StringBuilder()
            geminiDataSource.sendMessage(historyBefore, userPrompt, imageBitmap)
                .catch { e ->
                    Timber.e(e, "Gemini streaming error: ${e.javaClass.simpleName}")
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
            Timber.d("Gemini streaming completed, response length=${fullResponse.length}")

            // Persist the completed AI response
            repository.saveMessage(
                ChatMessage(
                    sessionId = sessionId,
                    content = fullResponse.toString(),
                    role = MessageRole.MODEL,
                    timestamp = System.currentTimeMillis(),
                )
            )
            Timber.d("AI response saved to repository")

            // Auto-title the session from the user's first message (truncated to 60 chars)
            if (isFirstMessage) {
                Timber.i("Auto-titling session from first message")
                updateSessionTitle(sessionId, userPrompt.take(60))
            }
        } catch (e: DomainError) {
            Timber.e(e, "DomainError in SendMessageUseCase")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in SendMessageUseCase")
            throw DomainError.DatabaseError("Failed to send message: ${e.message}")
        }
    }
}
