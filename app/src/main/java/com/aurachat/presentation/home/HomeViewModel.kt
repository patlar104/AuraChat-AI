package com.aurachat.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurachat.domain.error.DomainError
import com.aurachat.domain.usecase.CreateSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createSession: CreateSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isCreatingSession) return
        createSessionAndNavigate(text)
    }

    fun onSuggestionTapped(suggestionText: String) {
        if (_uiState.value.isCreatingSession) return
        createSessionAndNavigate(suggestionText)
    }

    /** Called by the composable after it has consumed the navigation event. */
    fun onNavigationConsumed() {
        _uiState.update { it.copy(navigateToSessionId = null) }
    }

    /** Called by the composable after the error snackbar is dismissed. */
    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun createSessionAndNavigate(title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSession = true) }
            try {
                val sessionId = createSession(initialTitle = title.take(60))
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        navigateToSessionId = sessionId,
                        inputText = "",
                    )
                }
            } catch (e: DomainError) {
                val errorMessage = when (e) {
                    is DomainError.DatabaseError -> "Failed to create chat session. Please try again."
                    is DomainError.NetworkError -> "Network error. Please check your connection."
                    is DomainError.ApiError -> "AI service error. Please try again."
                    is DomainError.ValidationError -> e.message
                    is DomainError.UnknownError -> "Failed to start chat. Please try again."
                }
                _uiState.update {
                    it.copy(isCreatingSession = false, errorMessage = errorMessage)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isCreatingSession = false, errorMessage = "Failed to start chat. Please try again.")
                }
            }
        }
    }
}
