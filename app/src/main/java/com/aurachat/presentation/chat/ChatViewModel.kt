package com.aurachat.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurachat.domain.usecase.GetMessagesUseCase
import com.aurachat.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the chat screen.
 *
 * Loads and observes messages for a given session, manages the streaming state
 * during AI response generation, and exposes UI events for input and send actions.
 *
 * The [sessionId] is resolved from [SavedStateHandle] injected by Hilt Navigation.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    // NavRoutes.CHAT = "chat/{sessionId}" — key must match exactly
    /** The session ID this ViewModel is bound to, sourced from the navigation back-stack entry. */
    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Active send+stream coroutine job; cancel on retry
    private var sendJob: Job? = null

    init {
        observeMessages()
    }

    // ── Message observation ───────────────────────────────────────────────────

    private fun observeMessages() {
        getMessages(sessionId)
            .onEach { messages ->
                _uiState.update { state ->
                    state.copy(
                        messages = messages,
                        isLoadingMessages = false,
                        // STREAMING HANDOFF: clear streamingText atomically with the new
                        // message list only once streaming is done. This prevents the blank-
                        // bubble flicker: when Room emits after the AI message is saved,
                        // we set both messages and streamingText=null in the same update.
                        streamingText = if (state.isStreaming) state.streamingText else null,
                    )
                }
            }
            .catch { e ->
                // Room errors are non-recoverable; log and swallow silently
                Timber.e(e, "Error observing messages for sessionId=%d", sessionId)
            }
            .launchIn(viewModelScope)
    }

    // ── User interactions ─────────────────────────────────────────────────────

    /**
     * Updates the text input field as the user types. Also clears any visible error message.
     *
     * @param text The current value of the input field.
     */
    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    /**
     * Sends the current input text to the AI and starts streaming the response.
     *
     * No-ops if the input is blank or a response is already streaming.
     */
    fun onSendClicked() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isBlank() || _uiState.value.isStreaming) return
        startSend(prompt)
    }

    /**
     * Clears the current error message, allowing the user to re-type and retry.
     */
    fun onRetryClicked() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Internal send logic ───────────────────────────────────────────────────

    private fun startSend(prompt: String) {
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    inputText = "",
                    isStreaming = true,
                    streamingText = "",  // Empty string = bubble visible, no chunks yet
                    errorMessage = null,
                )
            }

            try {
                sendMessage(sessionId, prompt).collect { chunk ->
                    _uiState.update { state ->
                        state.copy(streamingText = (state.streamingText ?: "") + chunk)
                    }
                }

                // Stream completed. SendMessageUseCase already saved the AI message to Room.
                // Set isStreaming=false but intentionally leave streamingText non-null.
                // observeMessages() will clear it atomically when Room fires with the saved message.
                _uiState.update { state ->
                    state.copy(isStreaming = false)
                }

            } catch (e: Exception) {
                Timber.e(e, "Streaming error for sessionId=%d", sessionId)
                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        streamingText = null,
                        errorMessage = e.message ?: "Something went wrong. Please try again.",
                        inputText = prompt, // restore prompt so user can retry without retyping
                    )
                }
            }
        }
    }
}
