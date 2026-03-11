package com.aurachat.data.remote

import android.graphics.Bitmap
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.util.Constants
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase AI (Gemini) implementation of [GeminiDataSource].
 *
 * Constructs a chat session from prior history, initiates a streaming request, and
 * emits non-empty text chunks as they arrive from the model. History is capped at
 * [Constants.Gemini.HISTORY_LIMIT] messages and error messages are excluded to avoid
 * confusing the model with failed responses.
 *
 * When [imageBitmap] is provided, the request is sent as a multimodal vision query
 * using [GenerativeModel.generateContentStream] with the full history included manually,
 * since the Chat session API accepts text-only prompts.
 */
@Singleton
class GeminiDataSourceImpl @Inject constructor(
    private val model: GenerativeModel,
) : GeminiDataSource {

    override fun sendMessage(
        history: List<ChatMessage>,
        userPrompt: String,
        imageBitmap: Bitmap?,
    ): Flow<String> {
        Timber.d("Starting Gemini stream: historySize=%d hasImage=%b prompt=%s",
            history.size, imageBitmap != null, userPrompt.take(80))

        return if (imageBitmap != null) {
            // Vision path: build the full content list (history + image + text) and
            // pass it to generateContentStream, which accepts multimodal Content objects.
            val allContent = buildChatHistory(history) + listOf(
                content("user") {
                    image(imageBitmap)
                    text(userPrompt)
                }
            )
            model.generateContentStream(*allContent.toTypedArray())
                .map { response -> response.text ?: "" }
                .filter { it.isNotEmpty() }
        } else {
            // Text-only path: use the Chat session API for clean conversation management
            val chat = model.startChat(history = buildChatHistory(history))
            chat.sendMessageStream(userPrompt)
                .map { response -> response.text ?: "" }
                .filter { it.isNotEmpty() }
        }
    }

    /**
     * Maps domain [ChatMessage] list to Firebase [Content] list for `startChat(history)`
     * or multimodal `generateContentStream` calls.
     *
     * Rules:
     * - [MessageRole.USER] messages → role `"user"`
     * - [MessageRole.MODEL] messages → role `"model"`
     * - Error messages ([ChatMessage.isError] = true) are excluded — sending a failed AI
     *   response back as history confuses the model and wastes context window.
     * - History is capped at [Constants.Gemini.HISTORY_LIMIT] messages to avoid
     *   exceeding token limits on long conversations.
     */
    private fun buildChatHistory(messages: List<ChatMessage>): List<Content> =
        messages
            .filter { !it.isError }
            .takeLast(Constants.Gemini.HISTORY_LIMIT)
            .map { msg ->
                content(role = if (msg.role == MessageRole.USER) "user" else "model") {
                    text(msg.content)
                }
            }
}
