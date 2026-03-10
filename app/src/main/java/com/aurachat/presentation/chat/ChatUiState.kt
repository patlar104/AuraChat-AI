package com.aurachat.presentation.chat

import com.aurachat.domain.model.ChatMessage

data class ChatUiState(
    // Persisted messages from Room — never includes the active streaming message
    val messages: List<ChatMessage> = emptyList(),

    // null = no stream active; "" = stream started but no chunks yet; non-empty = live text
    val streamingText: String? = null,

    // True while SendMessageUseCase is actively collecting chunks from Gemini
    val isStreaming: Boolean = false,

    // Current value of the input TextField
    val inputText: String = "",

    // Non-null when the last send attempt failed
    val errorMessage: String? = null,

    // True until the first Room emission arrives on screen open
    val isLoadingMessages: Boolean = true,
)
