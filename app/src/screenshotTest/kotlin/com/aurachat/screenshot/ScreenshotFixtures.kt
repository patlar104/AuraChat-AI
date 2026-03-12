package com.aurachat.screenshot

import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.model.MessageRole
import com.aurachat.presentation.chat.ChatUiState
import com.aurachat.presentation.history.HistoryUiState
import com.aurachat.presentation.home.HomeUiState
import com.aurachat.presentation.settings.SettingsUiState

private const val FIXED_NOW = 1_700_000_000_000L
private const val USER_MESSAGE_TIME = FIXED_NOW - 120_000L
private const val MODEL_MESSAGE_TIME = FIXED_NOW - 60_000L

object ScreenshotFixtures {
    val homeDefault = HomeUiState()

    val chatConversation = ChatUiState(
        messages = listOf(
            ChatMessage(
                id = 1L,
                sessionId = 10L,
                content = "Plan a focused Kotlin test refactor.",
                role = MessageRole.USER,
                timestamp = USER_MESSAGE_TIME,
            ),
            ChatMessage(
                id = 2L,
                sessionId = 10L,
                content = "Start with coroutine rules, deterministic fakes, and a few smoke tests.",
                role = MessageRole.MODEL,
                timestamp = MODEL_MESSAGE_TIME,
            ),
        ),
        isLoadingMessages = false,
    )

    val chatError = ChatUiState(
        messages = chatConversation.messages,
        errorMessage = "Network error. Please check your connection.",
        isLoadingMessages = false,
    )

    val historyEmpty = HistoryUiState(
        sessions = emptyList(),
        isLoading = false,
    )

    val historyPopulated = HistoryUiState(
        sessions = listOf(
            ChatSession(
                id = 42L,
                title = "Release checklist",
                createdAt = FIXED_NOW - 1_200_000L,
                updatedAt = FIXED_NOW - 300_000L,
                messageCount = 3,
                lastMessagePreview = "Ship the screenshot tests after smoke coverage is green.",
            ),
            ChatSession(
                id = 7L,
                title = "Bug triage",
                createdAt = FIXED_NOW - 7_200_000L,
                updatedAt = FIXED_NOW - 3_600_000L,
                messageCount = 8,
                lastMessagePreview = "Retry should preserve the failed prompt and image attachment.",
            ),
        ),
        isLoading = false,
    )

    val settingsDefault = SettingsUiState(selectedModel = "gemini-2.0-flash")

    fun fixedRelativeTime(timestamp: Long): String = when (timestamp) {
        FIXED_NOW - 300_000L -> "5m ago"
        FIXED_NOW - 3_600_000L -> "1h ago"
        else -> "just now"
    }
}
