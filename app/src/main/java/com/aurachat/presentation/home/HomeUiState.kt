package com.aurachat.presentation.home

data class HomeUiState(
    val inputText: String = "",
    /** Non-null while a navigation to chat is pending. Reset via onNavigationConsumed(). */
    val navigateToSessionId: Long? = null,
    /** True while CreateSessionUseCase is in-flight — prevents re-entrant taps. */
    val isCreatingSession: Boolean = false,
    /** Non-null when session creation failed. Reset via onErrorDismissed(). */
    val errorMessage: String? = null,
)
