package com.aurachat.presentation.settings

import com.aurachat.domain.repository.SettingsRepository
import com.aurachat.testutil.MainDispatcherExtension
import com.aurachat.testutil.SharedFlowSubject
import com.aurachat.testutil.runTest
import com.aurachat.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var modelUpdates: SharedFlowSubject<String>
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        settingsRepository = mockk()
        modelUpdates = SharedFlowSubject()
        every { settingsRepository.selectedModel } returns modelUpdates.flow
        viewModel = SettingsViewModel(settingsRepository)
    }

    @Test
    fun `ui state uses the default model before the repository emits`() = mainDispatcher.runTest {
        assertEquals(Constants.Gemini.DEFAULT_MODEL, viewModel.uiState.value.selectedModel)
    }

    @Test
    fun `stateIn-backed ui state updates while an active collector is present`() = mainDispatcher.runTest {
        backgroundScope.launch { viewModel.uiState.collect { } }
        mainDispatcher.scheduler.runCurrent()

        modelUpdates.tryEmit("gemini-1.5-pro")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals("gemini-1.5-pro", viewModel.uiState.value.selectedModel)

        modelUpdates.tryEmit("gemini-2.5-flash")
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals("gemini-2.5-flash", viewModel.uiState.value.selectedModel)
    }

    @Test
    fun `setSelectedModel delegates to the repository`() = mainDispatcher.runTest {
        coEvery { settingsRepository.setSelectedModel("gemini-1.5-flash") } returns Unit

        viewModel.setSelectedModel("gemini-1.5-flash")
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setSelectedModel("gemini-1.5-flash") }
    }
}
