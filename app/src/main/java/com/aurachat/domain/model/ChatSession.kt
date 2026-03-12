package com.aurachat.domain.model

/**
 * Domain model representing a chat session (conversation thread).
 *
 * A session is created when the user initiates a new chat. Its title is initially
 * set from the user's first prompt (truncated to [com.aurachat.util.Constants.Session.MAX_TITLE_LENGTH])
 * and can be updated later.
 *
 * @property id Unique database-generated identifier. Defaults to 0 before persistence.
 * @property title Display name shown in the history drawer.
 * @property createdAt Unix epoch milliseconds when the session was created.
 * @property updatedAt Unix epoch milliseconds of the last message in this session.
 *   Used to sort sessions newest-first in the drawer.
 * @property messageCount Denormalized count of messages for display without a JOIN.
 * @property lastMessagePreview Truncated text of the most recent message, shown as a
 *   subtitle in the history list.
 * @property pendingInitialPrompt Durable startup prompt for a newly-created session.
 *   Claimed once by the chat screen and then cleared from persistence.
 */
data class ChatSession(
    val id: Long = 0L,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val lastMessagePreview: String = "",
    val pendingInitialPrompt: String? = null,
)
