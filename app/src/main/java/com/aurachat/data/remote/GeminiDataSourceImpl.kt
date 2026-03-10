package com.aurachat.data.remote

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
 */
@Singleton
class GeminiDataSourceImpl @Inject constructor(
    private val model: GenerativeModel,
) : GeminiDataSource {

    override fun sendMessage(history: List<ChatMessage>, userPrompt: String): Flow<String> {
        Timber.d("Starting Gemini stream: historySize=%d prompt=%s", history.size, userPrompt.take(80))
        val chat = model.startChat(history = buildChatHistory(history))
        return chat.sendMessageStream(userPrompt)
            .map { response -> response.text ?: "" }
            .filter { it.isNotEmpty() }
    }

    /**
     * Maps domain [ChatMessage] list to Firebase [Content] list for `startChat(history)`.
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
