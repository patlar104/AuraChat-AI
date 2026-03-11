package com.aurachat.presentation.settings

import com.aurachat.util.Constants

data class SettingsUiState(
    val selectedModel: String = Constants.Gemini.DEFAULT_MODEL,
)
