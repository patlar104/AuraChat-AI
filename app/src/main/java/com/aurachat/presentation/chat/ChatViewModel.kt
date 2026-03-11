package com.aurachat.presentation.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurachat.domain.usecase.GetMessagesUseCase
import com.aurachat.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
    private val getMessages: GetMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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
                        streamingText = if (state.isStreaming) state.streamingText else null,
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Error observing messages for sessionId=%d", sessionId)
            }
            .launchIn(viewModelScope)
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
                    inputText = "",
                    pendingImageUri = null,
                    isStreaming = true,
                    streamingText = "",
                    errorMessage = null,
                )
            }

            // Decode the selected image to a Bitmap on IO dispatcher
            val bitmap: Bitmap? = imageUri?.let { uri ->
                withContext(Dispatchers.IO) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                decoder.isMutableRequired = true
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to decode selected image — sending text only")
                        null
                    }
                }
            }

            try {
                sendMessage(sessionId, prompt, bitmap).collect { chunk ->
                    _uiState.update { state ->
                        state.copy(streamingText = (state.streamingText ?: "") + chunk)
                    }
                }

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
                        inputText = prompt,
                        lastFailedPrompt = prompt,
                        lastFailedImageUri = imageUri,
                    )
                }
            }
        }
    }
}
