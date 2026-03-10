package com.aurachat.presentation.home

import app.cash.turbine.test
import com.aurachat.domain.usecase.CreateSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var createSessionUseCase: CreateSessionUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        createSessionUseCase = mockk()
        viewModel = HomeViewModel(createSessionUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.inputText)
            assertNull(state.navigateToSessionId)
            assertFalse(state.isCreatingSession)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `onInputChanged updates inputText`() = runTest {
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Hello")
            assertEquals("Hello", awaitItem().inputText)

            viewModel.onInputChanged("Hello World")
            assertEquals("Hello World", awaitItem().inputText)
        }
    }

    @Test
    fun `onSend creates session with valid input`() = runTest {
        val expectedSessionId = 123L
        coEvery { createSessionUseCase(any()) } returns expectedSessionId

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test message")
            awaitItem() // inputText update

            viewModel.onSend()

            // Should set isCreatingSession to true
            val creatingState = awaitItem()
            assertTrue(creatingState.isCreatingSession)
            assertEquals("Test message", creatingState.inputText)

            advanceUntilIdle()

            // Should complete with navigation and clear input
            val completedState = awaitItem()
            assertFalse(completedState.isCreatingSession)
            assertEquals(expectedSessionId, completedState.navigateToSessionId)
            assertEquals("", completedState.inputText)
            assertNull(completedState.errorMessage)
        }

        coVerify { createSessionUseCase("Test message") }
    }

    @Test
    fun `onSend with blank input does nothing`() = runTest {
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("   ")
            awaitItem() // inputText update

            viewModel.onSend()

            // Should not create session or emit new state
            expectNoEvents()
        }

        coVerify(exactly = 0) { createSessionUseCase(any()) }
    }

    @Test
    fun `onSend with empty input does nothing`() = runTest {
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onSend()

            // Should not create session or emit new state
            expectNoEvents()
        }

        coVerify(exactly = 0) { createSessionUseCase(any()) }
    }

    @Test
    fun `onSend when already creating session does nothing`() = runTest {
        // Use CompletableDeferred so the first session stays in-progress while we test the guard
        val sessionCreated = CompletableDeferred<Long>()
        coEvery { createSessionUseCase(any()) } coAnswers { sessionCreated.await() }

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("First message")
            awaitItem() // inputText update

            // Start first session creation
            viewModel.onSend()
            awaitItem() // isCreatingSession = true

            // Session is blocked at sessionCreated.await() — isCreatingSession = true
            // Try to send again before first completes
            viewModel.onInputChanged("Second message")
            awaitItem() // inputText update — isCreatingSession still true

            viewModel.onSend() // no-op: isCreatingSession = true

            // Release the first session
            sessionCreated.complete(123L)
            advanceUntilIdle()

            // Should only emit the completion of first session
            val completedState = awaitItem()
            assertEquals(123L, completedState.navigateToSessionId)
            assertEquals("", completedState.inputText)

            expectNoEvents()
        }

        // Should only be called once
        coVerify(exactly = 1) { createSessionUseCase("First message") }
    }

    @Test
    fun `onSend trims whitespace`() = runTest {
        val expectedSessionId = 456L
        coEvery { createSessionUseCase(any()) } returns expectedSessionId

        viewModel.onInputChanged("  Test with spaces  ")
        viewModel.onSend()

        advanceUntilIdle()

        coVerify { createSessionUseCase("Test with spaces") }
    }

    @Test
    fun `onSend truncates title to 60 characters`() = runTest {
        val longText = "a".repeat(100)
        val expectedSessionId = 789L
        coEvery { createSessionUseCase(any()) } returns expectedSessionId

        viewModel.onInputChanged(longText)
        viewModel.onSend()

        advanceUntilIdle()

        coVerify { createSessionUseCase(longText.take(60)) }
    }

    @Test
    fun `onSuggestionTapped creates session with suggestion text`() = runTest {
        val expectedSessionId = 999L
        coEvery { createSessionUseCase(any()) } returns expectedSessionId

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onSuggestionTapped("What is AI?")

            // Should set isCreatingSession to true
            val creatingState = awaitItem()
            assertTrue(creatingState.isCreatingSession)

            advanceUntilIdle()

            // Should complete with navigation
            val completedState = awaitItem()
            assertFalse(completedState.isCreatingSession)
            assertEquals(expectedSessionId, completedState.navigateToSessionId)
            assertEquals("", completedState.inputText)
        }

        coVerify { createSessionUseCase("What is AI?") }
    }

    @Test
    fun `onSuggestionTapped when already creating session does nothing`() = runTest {
        // Use CompletableDeferred so the first session stays in-progress while we test the guard
        val sessionCreated = CompletableDeferred<Long>()
        coEvery { createSessionUseCase(any()) } coAnswers { sessionCreated.await() }

        viewModel.uiState.test {
            awaitItem() // skip initial

            // Start first session creation via onSend
            viewModel.onInputChanged("First message")
            awaitItem() // inputText update

            viewModel.onSend()
            awaitItem() // isCreatingSession = true

            // Session is blocked at sessionCreated.await() — isCreatingSession = true
            // Try to tap suggestion before first completes
            viewModel.onSuggestionTapped("Suggestion") // no-op: isCreatingSession = true

            // Release the first session
            sessionCreated.complete(123L)
            advanceUntilIdle()

            awaitItem() // completion of first session

            expectNoEvents()
        }

        // Should only be called once
        coVerify(exactly = 1) { createSessionUseCase("First message") }
    }

    @Test
    fun `onSuggestionTapped truncates title to 60 characters`() = runTest {
        val longSuggestion = "b".repeat(100)
        val expectedSessionId = 555L
        coEvery { createSessionUseCase(any()) } returns expectedSessionId

        viewModel.onSuggestionTapped(longSuggestion)

        advanceUntilIdle()

        coVerify { createSessionUseCase(longSuggestion.take(60)) }
    }

    @Test
    fun `onNavigationConsumed clears navigateToSessionId`() = runTest {
        val expectedSessionId = 123L
        coEvery { createSessionUseCase(any()) } returns expectedSessionId

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSend()
            awaitItem() // isCreatingSession = true

            advanceUntilIdle()
            val stateWithNav = awaitItem()
            assertEquals(expectedSessionId, stateWithNav.navigateToSessionId)

            viewModel.onNavigationConsumed()
            val stateAfterConsume = awaitItem()
            assertNull(stateAfterConsume.navigateToSessionId)
        }
    }

    @Test
    fun `onErrorDismissed clears errorMessage`() = runTest {
        coEvery { createSessionUseCase(any()) } throws RuntimeException("Test error")

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSend()
            awaitItem() // isCreatingSession = true

            advanceUntilIdle()
            val stateWithError = awaitItem()
            assertEquals("Failed to start chat. Please try again.", stateWithError.errorMessage)

            viewModel.onErrorDismissed()
            val stateAfterDismiss = awaitItem()
            assertNull(stateAfterDismiss.errorMessage)
        }
    }

    @Test
    fun `createSession error sets error message and stops loading`() = runTest {
        coEvery { createSessionUseCase(any()) } throws RuntimeException("Network error")

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test message")
            awaitItem() // inputText update

            viewModel.onSend()

            // Should set isCreatingSession to true
            val creatingState = awaitItem()
            assertTrue(creatingState.isCreatingSession)

            advanceUntilIdle()

            // Should set error and clear loading state
            val errorState = awaitItem()
            assertFalse(errorState.isCreatingSession)
            assertEquals("Failed to start chat. Please try again.", errorState.errorMessage)
            assertNull(errorState.navigateToSessionId)
            // Input text should remain for retry
            assertEquals("Test message", errorState.inputText)
        }
    }

    @Test
    fun `error does not clear input text for retry`() = runTest {
        coEvery { createSessionUseCase(any()) } throws RuntimeException("Error")

        viewModel.onInputChanged("Important message")
        viewModel.onSend()

        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Important message", state.inputText)
            assertEquals("Failed to start chat. Please try again.", state.errorMessage)
        }
    }

    @Test
    fun `successful session creation after error recovery`() = runTest {
        var callCount = 0
        coEvery { createSessionUseCase(any()) } answers {
            if (callCount++ == 0) {
                throw RuntimeException("First attempt failed")
            } else {
                123L
            }
        }

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            // First attempt - should fail
            viewModel.onSend()
            awaitItem() // isCreatingSession = true
            advanceUntilIdle()
            val errorState = awaitItem()
            assertEquals("Failed to start chat. Please try again.", errorState.errorMessage)
            assertEquals("Test", errorState.inputText)

            // Dismiss error
            viewModel.onErrorDismissed()
            awaitItem() // errorMessage cleared

            // Second attempt - should succeed
            viewModel.onSend()
            awaitItem() // isCreatingSession = true
            advanceUntilIdle()
            val successState = awaitItem()
            assertEquals(123L, successState.navigateToSessionId)
            assertEquals("", successState.inputText)
            assertNull(successState.errorMessage)
        }

        coVerify(exactly = 2) { createSessionUseCase("Test") }
    }
}
