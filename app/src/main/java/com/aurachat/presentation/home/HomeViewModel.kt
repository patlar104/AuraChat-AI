package com.aurachat.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurachat.domain.usecase.CreateSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Home screen that manages chat session creation.
 *
 * Handles user input, suggestion chips, and orchestrates the creation of new chat sessions
 * with automatic navigation to the newly created session. Provides error handling and
 * loading state management for the session creation flow.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createSession: CreateSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        Timber.d("HomeViewModel initialized")
    }

    /**
     * Updates the input text state as the user types.
     *
     * @param text The current input text from the text field
     */
    fun onInputChanged(text: String) {
        Timber.d("Input changed: ${text.take(20)}${if (text.length > 20) "..." else ""}")
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Handles the send button click, creating a new session with the current input text.
     *
     * Validates that the input is not blank and that a session creation is not already in progress.
     * On success, navigates to the newly created chat session.
     */
    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isCreatingSession) {
            Timber.d("onSend ignored: blank=${text.isBlank()}, isCreating=${_uiState.value.isCreatingSession}")
            return
        }
        Timber.i("User initiated send from home with text: ${text.take(30)}...")
        createSessionAndNavigate(text)
    }

    /**
     * Handles suggestion chip taps, creating a new session with the suggestion text.
     *
     * Validates that a session creation is not already in progress. On success,
     * navigates to the newly created chat session.
     *
     * @param suggestionText The text from the tapped suggestion chip
     */
    fun onSuggestionTapped(suggestionText: String) {
        if (_uiState.value.isCreatingSession) {
            Timber.d("Suggestion tap ignored: already creating session")
            return
        }
        Timber.i("User tapped suggestion: ${suggestionText.take(30)}...")
        createSessionAndNavigate(suggestionText)
    }

    /**
     * Clears the navigation event after the composable has consumed it.
     *
     * Should be called by the composable after successfully navigating to the chat screen
     * to prevent repeated navigation attempts.
     */
    fun onNavigationConsumed() {
        Timber.d("Navigation event consumed")
        _uiState.update { it.copy(navigateToSessionId = null) }
    }

    /**
     * Clears the error state after the user dismisses the error message.
     *
     * Should be called by the composable after the error snackbar is dismissed
     * to clear the error from the UI state.
     */
    fun onErrorDismissed() {
        Timber.d("Error dismissed")
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun createSessionAndNavigate(title: String) {
        viewModelScope.launch {
            Timber.d("Creating session with title: ${title.take(30)}...")
            _uiState.update { it.copy(isCreatingSession = true) }
            try {
                val sessionId = createSession(initialTitle = title.take(60))
                Timber.d("Session created successfully with id=$sessionId")
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        navigateToSessionId = sessionId,
                        inputText = "",
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create session: ${e.javaClass.simpleName}")
                _uiState.update {
                    it.copy(isCreatingSession = false, errorMessage = "Failed to start chat. Please try again.")
                }
            }
        }
    }
}
