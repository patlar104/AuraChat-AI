package com.aurachat.data.remote

import android.graphics.Bitmap
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.repository.SettingsRepository
import com.aurachat.util.Constants
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase AI (Gemini) implementation of [GeminiDataSource].
 *
 * The active model name is read from [SettingsRepository] on each [sendMessage] call so
 * that switching models in Settings takes effect immediately for the next conversation.
 *
 * History is capped at [Constants.Gemini.HISTORY_LIMIT] messages. Error messages are
 * excluded from history to avoid confusing the model with failed responses.
 *
 * Vision requests use [GenerativeModel.generateContentStream] with the full history +
 * image + text, since the Chat session API accepts text-only prompts.
 */
@Singleton
class GeminiDataSourceImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : GeminiDataSource {

    override fun sendMessage(
        history: List<ChatMessage>,
        userPrompt: String,
        imageBitmap: Bitmap?,
    ): Flow<String> = flow {
        val modelName = settingsRepository.selectedModel.first()
        val model = createModel(modelName)

        Timber.d(
            "Starting Gemini stream: model=%s historySize=%d hasImage=%b prompt=%s",
            modelName, history.size, imageBitmap != null, userPrompt.take(80),
        )

        val upstream = if (imageBitmap != null) {
            // Vision path: build the full content list (history + image + text) and
            // pass it to generateContentStream, which accepts multimodal Content objects.
            val allContent = buildChatHistory(history) + listOf(
                content("user") {
                    image(imageBitmap)
                    text(userPrompt)
                }
            )
            model.generateContentStream(allContent)
        } else {
            // Text-only path: use the Chat session API for clean conversation management.
            val chat = model.startChat(history = buildChatHistory(history))
            chat.sendMessageStream(userPrompt)
        }

        emitAll(
            upstream
                .map { response -> response.text ?: "" }
                .filter { it.isNotEmpty() }
        )
    }

    private fun createModel(modelName: String): GenerativeModel =
        FirebaseAI.getInstance(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    temperature = Constants.Gemini.TEMPERATURE
                    topK = Constants.Gemini.TOP_K
                    topP = Constants.Gemini.TOP_P
                    maxOutputTokens = Constants.Gemini.MAX_OUTPUT_TOKENS
                },
            )

    /**
     * Maps domain [ChatMessage] list to Firebase [Content] list for use in
     * `startChat(history)` or multimodal `generateContentStream` calls.
     *
     * - Error messages ([ChatMessage.isError] = true) are excluded
     * - History is capped at [Constants.Gemini.HISTORY_LIMIT] to limit token usage
     */
    private fun buildChatHistory(messages: List<ChatMessage>): List<Content> =
        messages
            .filter { !it.isError }
            .takeLast(Constants.Gemini.HISTORY_LIMIT)
            .map { msg ->
                content(role = if (msg.role == MessageRole.USER) "user" else "model") {
                    val historyText = buildString {
                        if (!msg.imageUri.isNullOrBlank()) {
                            append("[Image attached]")
                            if (msg.content.isNotBlank()) append('\n')
                        }
                        append(msg.content)
                    }
                    text(historyText)
                }
            }
}
