package com.aurachat.domain.model

data class ChatSession(
    val id: Long = 0L,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val lastMessagePreview: String = ""
)
