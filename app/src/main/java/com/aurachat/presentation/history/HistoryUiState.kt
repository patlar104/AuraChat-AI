package com.aurachat.presentation.history

import com.aurachat.domain.model.ChatSession

/**
 * UI state for the navigation drawer history list.
 *
 * @property sessions The ordered list of past sessions (newest first).
 * @property isLoading True until the first emission from [GetSessionsUseCase] arrives.
 */
data class HistoryUiState(
    val sessions: List<ChatSession> = emptyList(),
    val isLoading: Boolean = true,
)
