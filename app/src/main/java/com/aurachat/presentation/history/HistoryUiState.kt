package com.aurachat.presentation.history

import com.aurachat.domain.model.ChatSession

data class HistoryUiState(
    val sessions: List<ChatSession> = emptyList(),
    val isLoading: Boolean = true,
)
