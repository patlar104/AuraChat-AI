package com.aurachat.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurachat.domain.usecase.DeleteSessionUseCase
import com.aurachat.domain.usecase.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * ViewModel for the navigation drawer history list.
 *
 * Observes all chat sessions from [GetSessionsUseCase] and exposes them via [uiState].
 * Session deletion is fire-and-forget — Room's ForeignKey CASCADE constraint handles
 * removing the associated messages automatically.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getSessions: GetSessionsUseCase,
    private val deleteSession: DeleteSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        getSessions()
            .onEach { sessions ->
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
            .catch { e ->
                Timber.e(e, "Error loading session history")
                _uiState.update { it.copy(isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Permanently deletes the session identified by [sessionId] and all its messages.
     *
     * The operation is fire-and-forget; the [uiState] updates automatically when Room
     * emits the updated session list after deletion.
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            Timber.d("Deleting session id=%d", sessionId)
            deleteSession.invoke(sessionId)
        }
    }
}
