package com.aurachat.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurachat.R
import com.aurachat.domain.error.DomainError
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
 * ViewModel for the Chat screen that manages message sending, streaming, and display.
 *
 * Handles real-time message streaming from Gemini AI, manages the chat message history,
 * and coordinates the streaming handoff pattern to prevent UI flickering. Observes messages
 * from the repository and provides error handling for the send/receive flow.
 *
 * The streaming handoff ensures smooth transitions between streaming and persisted messages
 * by atomically updating both message list and streaming state when Room emits updates.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    // NavRoutes.CHAT = "chat/{sessionId}" — key must match exactly
    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Active send+stream coroutine job; cancel on retry
    private var sendJob: Job? = null

    init {
        Timber.d("ChatViewModel initialized for sessionId=$sessionId")
        observeMessages()
    }

    // ── Message observation ───────────────────────────────────────────────────

    private fun observeMessages() {
        Timber.d("Starting message observation for sessionId=$sessionId")
        getMessages(sessionId)
            .onEach { messages ->
                Timber.d("Received ${messages.size} messages from repository")
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
                Timber.e(e, "Error observing messages for sessionId=$sessionId")
            }
            .launchIn(viewModelScope)
    }

    // ── User interactions ─────────────────────────────────────────────────────

    /**
     * Updates the input text state as the user types and clears any error state.
     *
     * @param text The current input text from the text field
     */
    fun onInputChanged(text: String) {
        Timber.d("Input changed: ${text.take(20)}${if (text.length > 20) "..." else ""}")
        _uiState.update { it.copy(inputText = text, errorMessageResId = null) }
    }

    /**
     * Handles the send button click, initiating the message send and stream flow.
     *
     * Validates that the input is not blank and that a message is not already streaming.
     * Clears the input field and starts the streaming process for the AI response.
     */
    fun onSendClicked() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isBlank() || _uiState.value.isStreaming) {
            Timber.d("onSendClicked ignored: blank=${prompt.isBlank()}, isStreaming=${_uiState.value.isStreaming}")
            return
        }
        Timber.i("User sending message: ${prompt.take(30)}...")
        startSend(prompt)
    }

    /**
     * Clears the error state to allow the user to retry sending a message.
     *
     * Should be called when the user taps the retry button after a send failure.
     */
    fun onRetryClicked() {
        Timber.d("Retry clicked, clearing error state")
        _uiState.update { it.copy(errorMessageResId = null) }
    }

    // ── Internal send logic ───────────────────────────────────────────────────

    private fun startSend(prompt: String) {
        Timber.d("Starting send for sessionId=$sessionId")
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    inputText = "",
                    isStreaming = true,
                    streamingText = "",  // Empty string = bubble visible, no chunks yet
                    errorMessageResId = null,
                )
            }

            try {
                var chunkCount = 0
                sendMessage(sessionId, prompt).collect { chunk ->
                    chunkCount++
                    if (chunkCount == 1) {
                        Timber.d("Received first streaming chunk")
                    }
                    _uiState.update { state ->
                        state.copy(streamingText = (state.streamingText ?: "") + chunk)
                    }
                }

                Timber.d("Streaming completed with $chunkCount chunks")
                // Stream completed. SendMessageUseCase already saved the AI message to Room.
                // Set isStreaming=false but intentionally leave streamingText non-null.
                // observeMessages() will clear it atomically when Room fires with the saved message.
                _uiState.update { state ->
                    state.copy(isStreaming = false)
                }

            } catch (e: DomainError) {
                Timber.e(e, "Failed to send message: ${e.javaClass.simpleName}")
                val errorMessageResId = when (e) {
                    is DomainError.DatabaseError -> R.string.error_save_message
                    is DomainError.NetworkError -> R.string.error_network
                    is DomainError.ApiError -> R.string.error_api
                    is DomainError.ValidationError -> R.string.error_unknown
                    is DomainError.UnknownError -> R.string.error_unknown
                }
                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        streamingText = null,
                        errorMessageResId = errorMessageResId,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error sending message")
                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        streamingText = null,
                        errorMessageResId = R.string.error_unknown,
                    )
                }
            }
        }
    }
}
