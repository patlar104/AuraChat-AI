package com.aurachat.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aurachat.domain.repository.SettingsRepository
import com.aurachat.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences DataStore-backed implementation of [SettingsRepository].
 *
 * Persists settings to `settings.preferences_pb` in the app's private data directory.
 * All IO errors during reads are caught and replaced with defaults to prevent crashes.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private companion object {
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    override val selectedModel: Flow<String> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                Timber.e(e, "Error reading settings DataStore — using defaults")
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs -> prefs[KEY_SELECTED_MODEL] ?: Constants.Gemini.DEFAULT_MODEL }

    override suspend fun setSelectedModel(modelName: String) {
        Timber.d("Setting selected model to: %s", modelName)
        dataStore.edit { prefs -> prefs[KEY_SELECTED_MODEL] = modelName }
    }
}
