package com.aurachat.presentation.chat

import android.net.Uri
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

    // Non-null when the user has picked an image to attach to the next message
    val pendingImageUri: Uri? = null,

    // Stored when a send fails so the Retry button can re-send without re-typing
    val lastFailedPrompt: String? = null,
    val lastFailedImageUri: Uri? = null,
)
