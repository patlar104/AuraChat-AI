package com.aurachat.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for user-configurable app settings, backed by Preferences DataStore.
 *
 * All reads are exposed as [Flow] so the UI can reactively observe changes.
 * Writes are suspend functions to enforce off-main-thread execution.
 */
interface SettingsRepository {
    /** Emits the currently selected Gemini model name. Never null — defaults to gemini-2.5-flash. */
    val selectedModel: Flow<String>

    /** Persists [modelName] as the active Gemini model for future sessions. */
    suspend fun setSelectedModel(modelName: String)
}
