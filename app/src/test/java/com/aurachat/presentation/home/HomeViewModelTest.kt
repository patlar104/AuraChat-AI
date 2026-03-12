package com.aurachat.presentation.home

import com.aurachat.domain.usecase.StartChatSessionUseCase
import com.aurachat.testutil.MainDispatcherExtension
import com.aurachat.testutil.assertThrowsSuspend
import com.aurachat.testutil.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var startChatSessionUseCase: StartChatSessionUseCase
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setUp() {
        startChatSessionUseCase = mockk()
        viewModel = HomeViewModel(startChatSessionUseCase)
    }

    @Test
    fun `initial state is correct`() = mainDispatcher.runTest {
        assertEquals("", viewModel.uiState.value.inputText)
        assertNull(viewModel.uiState.value.navigateToSessionId)
        assertFalse(viewModel.uiState.value.isCreatingSession)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `successful send toggles loading and exposes navigation target`() = mainDispatcher.runTest {
        val sessionCreated = CompletableDeferred<Long>()
        coEvery { startChatSessionUseCase("Test message") } coAnswers { sessionCreated.await() }

        viewModel.onInputChanged("Test message")
        viewModel.onSend()
        mainDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isCreatingSession)
        assertEquals("Test message", viewModel.uiState.value.inputText)

        sessionCreated.complete(123L)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCreatingSession)
        assertEquals(123L, viewModel.uiState.value.navigateToSessionId)
        assertEquals("", viewModel.uiState.value.inputText)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `failed send preserves input and exposes retryable error`() = mainDispatcher.runTest {
        coEvery { startChatSessionUseCase("Important message") } throws IllegalStateException("boom")

        viewModel.onInputChanged("Important message")
        viewModel.onSend()
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCreatingSession)
        assertEquals("Important message", viewModel.uiState.value.inputText)
        assertEquals("Failed to start chat. Please try again.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.navigateToSessionId)
    }

    @Test
    fun `duplicate create is ignored while request is already in flight`() = mainDispatcher.runTest {
        val firstRequest = CompletableDeferred<Long>()
        coEvery { startChatSessionUseCase(any()) } coAnswers { firstRequest.await() }

        viewModel.onInputChanged("First message")
        viewModel.onSend()
        mainDispatcher.scheduler.runCurrent()

        viewModel.onInputChanged("Second message")
        viewModel.onSend()
        mainDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { startChatSessionUseCase("First message") }

        firstRequest.complete(456L)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(456L, viewModel.uiState.value.navigateToSessionId)
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `navigation consumed clears the pending navigation target`() = mainDispatcher.runTest {
        coEvery { startChatSessionUseCase("Test") } returns 999L

        viewModel.onInputChanged("Test")
        viewModel.onSend()
        mainDispatcher.scheduler.advanceUntilIdle()
        viewModel.onNavigationConsumed()

        assertNull(viewModel.uiState.value.navigateToSessionId)
    }

    @Test
    fun `blank input is ignored`() = mainDispatcher.runTest {
        viewModel.onInputChanged("   ")
        viewModel.onSend()
        mainDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isCreatingSession)
        coVerify(exactly = 0) { startChatSessionUseCase(any()) }
    }
}
