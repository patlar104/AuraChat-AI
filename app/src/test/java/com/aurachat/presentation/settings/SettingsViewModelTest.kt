package com.aurachat.presentation.settings

import app.cash.turbine.test
import com.aurachat.domain.repository.SettingsRepository
import com.aurachat.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(settingsRepository)

    @Test
    fun `initial uiState uses default model before repository emits`() = runTest {
        every { settingsRepository.selectedModel } returns flowOf()

        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Constants.Gemini.DEFAULT_MODEL, state.selectedModel)
        }
    }

    @Test
    fun `uiState reflects model emitted by repository`() = runTest {
        val model = "gemini-1.5-pro"
        every { settingsRepository.selectedModel } returns flowOf(model)

        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial value

            advanceUntilIdle()

            val loaded = awaitItem()
            assertEquals(model, loaded.selectedModel)
        }
    }

    @Test
    fun `uiState updates when repository emits a new model`() = runTest {
        every { settingsRepository.selectedModel } returns flow {
            emit("gemini-1.5-flash")
            emit("gemini-1.5-pro")
        }

        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial value

            advanceUntilIdle()

            val first = awaitItem()
            assertEquals("gemini-1.5-flash", first.selectedModel)

            val second = awaitItem()
            assertEquals("gemini-1.5-pro", second.selectedModel)
        }
    }

    @Test
    fun `setSelectedModel delegates to repository with correct model name`() = runTest {
        every { settingsRepository.selectedModel } returns flowOf(Constants.Gemini.DEFAULT_MODEL)
        coEvery { settingsRepository.setSelectedModel(any()) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setSelectedModel("gemini-1.5-pro")
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setSelectedModel("gemini-1.5-pro") }
    }

    @Test
    fun `setSelectedModel called multiple times delegates each call to repository`() = runTest {
        every { settingsRepository.selectedModel } returns flowOf(Constants.Gemini.DEFAULT_MODEL)
        coEvery { settingsRepository.setSelectedModel(any()) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setSelectedModel("gemini-1.5-flash")
        viewModel.setSelectedModel("gemini-1.5-pro")
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setSelectedModel("gemini-1.5-flash") }
        coVerify(exactly = 1) { settingsRepository.setSelectedModel("gemini-1.5-pro") }
    }
}
