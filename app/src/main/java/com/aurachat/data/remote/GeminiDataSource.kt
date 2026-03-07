package com.aurachat.data.remote

import com.aurachat.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface GeminiDataSource {
    /**
     * Streams a Gemini response for [userPrompt], given the prior [history].
     *
     * [history] should contain all messages BEFORE the current user prompt —
     * the prompt itself is passed separately to match the Firebase AI SDK contract.
     *
     * Emits text chunks as they arrive; completes when the model finishes.
     * Errors (network, safety block) propagate as Flow exceptions.
     */
    fun sendMessage(history: List<ChatMessage>, userPrompt: String): Flow<String>
}
