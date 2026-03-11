package com.aurachat.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.aurachat.util.Constants
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreSettingsRepository

    private val key = stringPreferencesKey("selected_model")

    @Before
    fun setup() {
        mockDataStore = mockk()
    }

    private fun createRepository(
        dataStore: DataStore<Preferences> = mockDataStore,
    ) = DataStoreSettingsRepository(dataStore)

    private fun TestScope.createRealDataStore(): DataStore<Preferences> {
        val directory = createTempDirectory(prefix = "settings-test-").toFile()
        val file = File(directory, "settings.preferences_pb")
        return PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file },
        )
    }

    @Test
    fun `selectedModel emits DEFAULT_MODEL when key is absent`() = runTest {
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        repository = createRepository()

        repository.selectedModel.test {
            assertEquals(Constants.Gemini.DEFAULT_MODEL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `selectedModel emits stored value when key is present`() = runTest {
        val storedModel = "gemini-1.5-pro"
        every { mockDataStore.data } returns flowOf(preferencesOf(key to storedModel))

        repository = createRepository()

        repository.selectedModel.test {
            assertEquals(storedModel, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `selectedModel emits DEFAULT_MODEL when IOException is thrown`() = runTest {
        every { mockDataStore.data } returns flow {
            throw IOException("disk read error")
        }

        repository = createRepository()

        repository.selectedModel.test {
            assertEquals(Constants.Gemini.DEFAULT_MODEL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `selectedModel rethrows non-IOException errors`() = runTest {
        every { mockDataStore.data } returns flow {
            throw IllegalStateException("unexpected")
        }

        repository = createRepository()

        repository.selectedModel.test {
            val error = awaitError()
            assert(error is IllegalStateException)
        }
    }

    @Test
    fun `setSelectedModel writes correct key-value pair to DataStore`() = runTest {
        val dataStore = createRealDataStore()
        repository = createRepository(dataStore)
        repository.setSelectedModel("gemini-1.5-flash")

        assertEquals("gemini-1.5-flash", dataStore.data.first()[key])
    }

    @Test
    fun `setSelectedModel overwrites previously stored model`() = runTest {
        val dataStore = createRealDataStore()
        dataStore.edit { prefs -> prefs[key] = "gemini-2.0-flash" }

        repository = createRepository(dataStore)
        repository.setSelectedModel("gemini-1.5-pro")

        assertEquals("gemini-1.5-pro", dataStore.data.first()[key])
    }
}
