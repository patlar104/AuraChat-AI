package com.aurachat.presentation.chat

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurachat.domain.usecase.ConsumePendingInitialPromptUseCase
import com.aurachat.domain.usecase.GetMessagesUseCase
import com.aurachat.domain.usecase.SendMessageUseCase
import com.aurachat.util.ImageAttachmentStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val consumePendingInitialPromptUseCase: ConsumePendingInitialPromptUseCase,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sendJob: Job? = null

    init {
        observeMessages()
        consumePendingInitialPrompt()
    }

    // ── Message observation ───────────────────────────────────────────────────

    private fun observeMessages() {
        getMessages(sessionId)
            .onEach { messages ->
                _uiState.update { state ->
                    state.copy(
                        messages = messages,
                        isLoadingMessages = false,
                        streamingText = if (state.isStreaming) state.streamingText else null,
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Error observing messages for sessionId=%d", sessionId)
            }
            .launchIn(viewModelScope)
    }

    private fun consumePendingInitialPrompt() {
        viewModelScope.launch {
            val prompt = consumePendingInitialPromptUseCase(sessionId)?.trim()
            if (!prompt.isNullOrBlank()) {
                Timber.d("Starting pending initial prompt for sessionId=%d", sessionId)
                startSend(prompt, imageUri = null)
            }
        }
    }

    // ── User interactions ─────────────────────────────────────────────────────

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun onSendClicked() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isBlank() || _uiState.value.isStreaming) return
        startSend(prompt, _uiState.value.pendingImageUri)
    }

    fun onRetryClicked() {
        val state = _uiState.value
        val prompt = state.lastFailedPrompt ?: return
        _uiState.update { it.copy(errorMessage = null, lastFailedPrompt = null, lastFailedImageUri = null) }
        startSend(prompt, state.lastFailedImageUri)
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(pendingImageUri = uri) }
    }

    fun onClearImage() {
        _uiState.update { it.copy(pendingImageUri = null) }
    }

    // ── Internal send logic ───────────────────────────────────────────────────

    private fun startSend(prompt: String, imageUri: Uri?) {
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isStreaming = true,
                    streamingText = null,
                    errorMessage = null,
                    lastFailedPrompt = null,
                    lastFailedImageUri = null,
                )
            }

            try {
                val preparedAttachment = imageUri?.let { selectedUri ->
                    prepareAttachment(selectedUri)
                }

                _uiState.update { state ->
                    state.copy(
                        inputText = "",
                        pendingImageUri = null,
                        streamingText = "",
                    )
                }

                sendMessage(
                    sessionId = sessionId,
                    userPrompt = prompt,
                    imageBitmap = preparedAttachment?.bitmap,
                    imageUri = preparedAttachment?.storedImageUri,
                ).collect { chunk ->
                    _uiState.update { state ->
                        state.copy(streamingText = (state.streamingText ?: "") + chunk)
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        streamingText = null,
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "Streaming error for sessionId=%d", sessionId)
                _uiState.update { state ->
                    state.copy(
                        isStreaming = false,
                        streamingText = null,
                        errorMessage = e.message ?: "Something went wrong. Please try again.",
                        inputText = prompt,
                        pendingImageUri = imageUri,
                        lastFailedPrompt = prompt,
                        lastFailedImageUri = imageUri,
                    )
                }
            }
        }
    }

    private suspend fun prepareAttachment(sourceUri: Uri): PreparedAttachment {
        val storedImageUri = ImageAttachmentStore.importSelectedImage(context, sourceUri)
        val bitmap = ImageAttachmentStore.decodeBitmap(context, storedImageUri)
            ?: error("Couldn't prepare that image. Try a different photo.")
        return PreparedAttachment(
            storedImageUri = storedImageUri,
            bitmap = bitmap,
        )
    }

    private data class PreparedAttachment(
        val storedImageUri: String,
        val bitmap: Bitmap,
    )
}
