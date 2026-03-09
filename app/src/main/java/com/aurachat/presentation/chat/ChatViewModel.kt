package com.aurachat.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

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
            .catch { /* Room errors are non-recoverable; swallow silently */ }
            .launchIn(viewModelScope)
    }

    // ── User interactions ─────────────────────────────────────────────────────

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun onSendClicked() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isBlank() || _uiState.value.isStreaming) return
        startSend(prompt)
    }

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

            } catch (e: DomainError) {
                val errorMessage = when (e) {
                    is DomainError.DatabaseError -> "Failed to save message. Please try again."
                    is DomainError.NetworkError -> "Network error. Please check your connection."
                    is DomainError.ApiError -> "AI service error. Please try again."
                    is DomainError.ValidationError -> e.message
                    is DomainError.UnknownError -> "Something went wrong. Please try again."
                }
                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        streamingText = null,
                        errorMessage = errorMessage,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        streamingText = null,
                        errorMessage = e.message ?: "Something went wrong. Please try again.",
                    )
                }
            }
        }
    }
}
