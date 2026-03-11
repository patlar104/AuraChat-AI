package com.aurachat.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.aurachat.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreSettingsRepository

    private val key = stringPreferencesKey("selected_model")

    @Before
    fun setup() {
        dataStore = mockk()
    }

    private fun createRepository() = DataStoreSettingsRepository(dataStore)

    @Test
    fun `selectedModel emits DEFAULT_MODEL when key is absent`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repository = createRepository()

        repository.selectedModel.test {
            assertEquals(Constants.Gemini.DEFAULT_MODEL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `selectedModel emits stored value when key is present`() = runTest {
        val storedModel = "gemini-1.5-pro"
        every { dataStore.data } returns flowOf(preferencesOf(key to storedModel))

        repository = createRepository()

        repository.selectedModel.test {
            assertEquals(storedModel, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `selectedModel emits DEFAULT_MODEL when IOException is thrown`() = runTest {
        every { dataStore.data } returns flow {
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
        every { dataStore.data } returns flow {
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
        val transformSlot = slot<suspend (MutablePreferences) -> Unit>()
        val fakePrefs = mutablePreferencesOf()
        coEvery { dataStore.edit(capture(transformSlot)) } coAnswers {
            transformSlot.captured.invoke(fakePrefs)
            fakePrefs
        }

        repository = createRepository()
        repository.setSelectedModel("gemini-1.5-flash")

        assertEquals("gemini-1.5-flash", fakePrefs[key])
        coVerify(exactly = 1) { dataStore.edit(any()) }
    }

    @Test
    fun `setSelectedModel overwrites previously stored model`() = runTest {
        val transformSlot = slot<suspend (MutablePreferences) -> Unit>()
        val fakePrefs = mutablePreferencesOf(key to "gemini-2.0-flash")
        coEvery { dataStore.edit(capture(transformSlot)) } coAnswers {
            transformSlot.captured.invoke(fakePrefs)
            fakePrefs
        }

        repository = createRepository()
        repository.setSelectedModel("gemini-1.5-pro")

        assertEquals("gemini-1.5-pro", fakePrefs[key])
    }
}
