package com.aurachat.domain.model

enum class MessageRole { USER, MODEL }

data class ChatMessage(
    val id: Long = 0L,
    val sessionId: Long,
    val content: String,
    val role: MessageRole,
    val timestamp: Long,
    val isStreaming: Boolean = false, // Runtime-only — never persisted
    val isError: Boolean = false
)
