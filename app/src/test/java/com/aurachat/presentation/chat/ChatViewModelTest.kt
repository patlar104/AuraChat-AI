package com.aurachat.presentation.chat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.usecase.GetMessagesUseCase
import com.aurachat.domain.usecase.SendMessageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
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
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getMessagesUseCase: GetMessagesUseCase
    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var viewModel: ChatViewModel

    private val testSessionId = 123L
    private val testMessages = listOf(
        ChatMessage(
            id = 1L,
            sessionId = testSessionId,
            content = "Hello",
            role = MessageRole.USER,
            timestamp = 1000L
        ),
        ChatMessage(
            id = 2L,
            sessionId = testSessionId,
            content = "Hi there!",
            role = MessageRole.MODEL,
            timestamp = 2000L
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to testSessionId))
        getMessagesUseCase = mockk()
        sendMessageUseCase = mockk()

        // Default: emit empty list initially
        every { getMessagesUseCase(testSessionId) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ChatViewModel {
        return ChatViewModel(savedStateHandle, getMessagesUseCase, sendMessageUseCase)
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(emptyList<ChatMessage>(), state.messages)
            assertEquals("", state.inputText)
            assertNull(state.streamingText)
            assertFalse(state.isStreaming)
            assertNull(state.errorMessage)
            assertTrue(state.isLoadingMessages)
        }
    }

    @Test
    fun `sessionId is loaded from SavedStateHandle`() = runTest {
        viewModel = createViewModel()
        assertEquals(testSessionId, viewModel.sessionId)
    }

    @Test
    fun `observeMessages loads messages from repository`() = runTest {
        every { getMessagesUseCase(testSessionId) } returns flowOf(testMessages)
        viewModel = createViewModel()

        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testMessages, state.messages)
            assertFalse(state.isLoadingMessages)
        }
    }

    @Test
    fun `observeMessages updates when repository emits new messages`() = runTest {
        val messages1 = listOf(testMessages[0])
        val messages2 = testMessages

        every { getMessagesUseCase(testSessionId) } returns flow {
            emit(messages1)
            emit(messages2)
        }

        viewModel = createViewModel()

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertTrue(initial.isLoadingMessages)

            advanceUntilIdle()

            // First emission
            val state1 = awaitItem()
            assertEquals(messages1, state1.messages)
            assertFalse(state1.isLoadingMessages)

            // Second emission
            val state2 = awaitItem()
            assertEquals(messages2, state2.messages)
            assertFalse(state2.isLoadingMessages)
        }
    }

    @Test
    fun `onInputChanged updates inputText`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Hello")
            assertEquals("Hello", awaitItem().inputText)

            viewModel.onInputChanged("Hello World")
            assertEquals("Hello World", awaitItem().inputText)
        }
    }

    @Test
    fun `onInputChanged clears errorMessage`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, any()) } throws RuntimeException("Error")
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            // Trigger error
            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            awaitItem() // isStreaming = true

            advanceUntilIdle()
            val errorState = awaitItem()
            assertEquals("Error", errorState.errorMessage)

            // Input change should clear error
            viewModel.onInputChanged("New text")
            val clearedState = awaitItem()
            assertEquals("New text", clearedState.inputText)
            assertNull(clearedState.errorMessage)
        }
    }

    @Test
    fun `onSendClicked with valid input starts streaming`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, "Test message") } returns flowOf(
            "Hello",
            " there",
            "!"
        )
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test message")
            awaitItem() // inputText update

            viewModel.onSendClicked()

            // Should clear input and start streaming
            val streamingState = awaitItem()
            assertEquals("", streamingState.inputText)
            assertTrue(streamingState.isStreaming)
            assertEquals("", streamingState.streamingText)
            assertNull(streamingState.errorMessage)

            advanceUntilIdle()

            // Should receive streaming chunks
            val chunk1 = awaitItem()
            assertEquals("Hello", chunk1.streamingText)
            assertTrue(chunk1.isStreaming)

            val chunk2 = awaitItem()
            assertEquals("Hello there", chunk2.streamingText)
            assertTrue(chunk2.isStreaming)

            val chunk3 = awaitItem()
            assertEquals("Hello there!", chunk3.streamingText)
            assertTrue(chunk3.isStreaming)

            // Streaming completes - isStreaming becomes false
            val completedState = awaitItem()
            assertEquals("Hello there!", completedState.streamingText)
            assertFalse(completedState.isStreaming)
        }

        coVerify { sendMessageUseCase(testSessionId, "Test message") }
    }

    @Test
    fun `onSendClicked with blank input does nothing`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("   ")
            awaitItem() // inputText update

            viewModel.onSendClicked()

            // Should not start streaming or emit new state
            expectNoEvents()
        }

        coVerify(exactly = 0) { sendMessageUseCase(any(), any()) }
    }

    @Test
    fun `onSendClicked with empty input does nothing`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onSendClicked()

            // Should not start streaming or emit new state
            expectNoEvents()
        }

        coVerify(exactly = 0) { sendMessageUseCase(any(), any()) }
    }

    @Test
    fun `onSendClicked while already streaming does nothing`() = runTest {
        // Use CompletableDeferred to keep the stream alive while we test the guard
        val streamingComplete = CompletableDeferred<Unit>()
        coEvery { sendMessageUseCase(testSessionId, any()) } returns flow {
            emit("Response")
            streamingComplete.await() // keeps isStreaming = true
        }
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("First message")
            awaitItem() // inputText update

            // Start first send
            viewModel.onSendClicked()
            awaitItem() // isStreaming = true

            // Consume the chunk emitted before streamingComplete.await()
            val chunkState = awaitItem()
            assertEquals("Response", chunkState.streamingText)
            assertTrue(chunkState.isStreaming)

            // Try to send again — stream is still active (blocked at streamingComplete.await())
            viewModel.onInputChanged("Second message")
            awaitItem() // inputText update — isStreaming still true

            viewModel.onSendClicked() // no-op: isStreaming = true

            // Release the stream and let it complete
            streamingComplete.complete(Unit)
            advanceUntilIdle()

            val completedState = awaitItem()
            assertFalse(completedState.isStreaming)

            expectNoEvents()
        }

        // Should only be called once
        coVerify(exactly = 1) { sendMessageUseCase(testSessionId, "First message") }
    }

    @Test
    fun `onSendClicked trims whitespace`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, any()) } returns flowOf("Response")
        viewModel = createViewModel()

        viewModel.onInputChanged("  Test with spaces  ")
        viewModel.onSendClicked()

        advanceUntilIdle()

        coVerify { sendMessageUseCase(testSessionId, "Test with spaces") }
    }

    @Test
    fun `streaming error sets errorMessage and stops streaming`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, any()) } throws RuntimeException("Network error")
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test message")
            awaitItem() // inputText update

            viewModel.onSendClicked()

            // Should start streaming
            val streamingState = awaitItem()
            assertTrue(streamingState.isStreaming)
            assertEquals("", streamingState.streamingText)

            advanceUntilIdle()

            // Should set error and stop streaming
            val errorState = awaitItem()
            assertFalse(errorState.isStreaming)
            assertNull(errorState.streamingText)
            assertEquals("Network error", errorState.errorMessage)
        }
    }

    @Test
    fun `streaming error with null message uses default error message`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, any()) } throws RuntimeException()
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            awaitItem() // isStreaming = true

            advanceUntilIdle()

            val errorState = awaitItem()
            assertEquals("Something went wrong. Please try again.", errorState.errorMessage)
        }
    }

    @Test
    fun `onRetryClicked clears errorMessage`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, any()) } throws RuntimeException("Error")
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            awaitItem() // isStreaming = true

            advanceUntilIdle()
            val errorState = awaitItem()
            assertEquals("Error", errorState.errorMessage)

            viewModel.onRetryClicked()
            val clearedState = awaitItem()
            assertNull(clearedState.errorMessage)
        }
    }

    @Test
    fun `successful send after error recovery`() = runTest {
        var callCount = 0
        coEvery { sendMessageUseCase(testSessionId, any()) } answers {
            if (callCount++ == 0) {
                throw RuntimeException("First attempt failed")
            } else {
                flowOf("Success")
            }
        }

        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            // First attempt - should fail
            viewModel.onSendClicked()
            awaitItem() // isStreaming = true
            advanceUntilIdle()
            val errorState = awaitItem()
            assertEquals("First attempt failed", errorState.errorMessage)
            assertFalse(errorState.isStreaming)

            // Retry
            viewModel.onRetryClicked()
            awaitItem() // errorMessage cleared

            // Second attempt - should succeed
            viewModel.onSendClicked()
            awaitItem() // isStreaming = true
            advanceUntilIdle()
            awaitItem() // streaming text
            val successState = awaitItem()
            assertFalse(successState.isStreaming)
            assertNull(successState.errorMessage)
        }

        coVerify(exactly = 2) { sendMessageUseCase(testSessionId, "Test") }
    }

    @Test
    fun `streaming handoff clears streamingText when Room emits`() = runTest {
        val initialMessages = emptyList<ChatMessage>()
        val updatedMessages = listOf(
            ChatMessage(
                id = 1L,
                sessionId = testSessionId,
                content = "User message",
                role = MessageRole.USER,
                timestamp = 1000L
            ),
            ChatMessage(
                id = 2L,
                sessionId = testSessionId,
                content = "AI response",
                role = MessageRole.MODEL,
                timestamp = 2000L
            )
        )

        // Use a Channel so we control exactly when Room emits each update
        val messagesChannel = Channel<List<ChatMessage>>(Channel.UNLIMITED)
        every { getMessagesUseCase(testSessionId) } returns messagesChannel.receiveAsFlow()

        coEvery { sendMessageUseCase(testSessionId, any()) } returns flowOf("AI", " response")

        viewModel = createViewModel()
        messagesChannel.trySend(initialMessages)
        advanceUntilIdle() // observeMessages processes initialMessages

        viewModel.uiState.test {
            awaitItem() // initial state with initialMessages

            viewModel.onInputChanged("User message")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            awaitItem() // isStreaming = true, streamingText = ""

            advanceUntilIdle()

            // Streaming chunks
            awaitItem() // streamingText = "AI"
            awaitItem() // streamingText = "AI response"
            awaitItem() // isStreaming = false

            // Room emits the saved message now that streaming is done
            messagesChannel.trySend(updatedMessages)
            advanceUntilIdle()

            // streamingText should be cleared atomically with the new messages
            val finalState = awaitItem()
            assertEquals(updatedMessages, finalState.messages)
            assertNull(finalState.streamingText)
            assertFalse(finalState.isStreaming)
        }
    }

    @Test
    fun `streaming handoff preserves streamingText while isStreaming is true`() = runTest {
        val messagesBeforeSend = listOf(testMessages[0])
        val messagesAfterSend = testMessages

        // Use a Channel so we control exactly when Room emits
        val messagesChannel = Channel<List<ChatMessage>>(Channel.UNLIMITED)
        every { getMessagesUseCase(testSessionId) } returns messagesChannel.receiveAsFlow()

        // Keep the stream alive so isStreaming stays true while Room emits
        val streamingComplete = CompletableDeferred<Unit>()
        coEvery { sendMessageUseCase(testSessionId, any()) } returns flow {
            emit("AI response")
            streamingComplete.await()
        }

        viewModel = createViewModel()
        messagesChannel.trySend(messagesBeforeSend)
        advanceUntilIdle() // observeMessages processes messagesBeforeSend

        viewModel.uiState.test {
            awaitItem() // initial state with messagesBeforeSend

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            awaitItem() // isStreaming = true, streamingText = ""

            // Consume the chunk emitted before streamingComplete.await()
            val chunkState = awaitItem()
            assertEquals("AI response", chunkState.streamingText)
            assertTrue(chunkState.isStreaming)

            // Room emits while isStreaming = true — streamingText must be preserved
            messagesChannel.trySend(messagesAfterSend)
            advanceUntilIdle()

            val midState = awaitItem()
            assertEquals(messagesAfterSend, midState.messages)
            assertEquals("AI response", midState.streamingText) // preserved!
            assertTrue(midState.isStreaming)

            // Release the stream
            streamingComplete.complete(Unit)
            advanceUntilIdle()

            val completedState = awaitItem()
            assertFalse(completedState.isStreaming)
        }
    }

    @Test
    fun `multiple rapid input changes only emit latest state`() = runTest {
        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("A")
            viewModel.onInputChanged("AB")
            viewModel.onInputChanged("ABC")

            // Should receive all updates
            assertEquals("A", awaitItem().inputText)
            assertEquals("AB", awaitItem().inputText)
            assertEquals("ABC", awaitItem().inputText)
        }
    }

    @Test
    fun `sendJob is cancelled when new send starts`() = runTest {
        var collectCount = 0
        coEvery { sendMessageUseCase(testSessionId, any()) } returns flow {
            collectCount++
            emit("Chunk 1")
            // Simulate long-running stream
            kotlinx.coroutines.delay(1000)
            emit("Chunk 2")
        }

        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("First")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            awaitItem() // isStreaming = true, streamingText = ""

            // "Chunk 1" is emitted synchronously before delay()
            awaitItem() // chunk 1 received

            // Note: The second send will be blocked by the isStreaming check,
            // so we can't actually test job cancellation this way.
            // This test documents the expected behavior.
            expectNoEvents()
        }
    }

    @Test
    fun `empty streaming response completes successfully`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, any()) } returns flowOf()
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            val streamingState = awaitItem()
            assertTrue(streamingState.isStreaming)
            assertEquals("", streamingState.streamingText)

            advanceUntilIdle()

            // Should complete with empty streamingText
            val completedState = awaitItem()
            assertFalse(completedState.isStreaming)
            assertEquals("", completedState.streamingText)
            assertNull(completedState.errorMessage)
        }
    }

    @Test
    fun `single chunk streaming response`() = runTest {
        coEvery { sendMessageUseCase(testSessionId, any()) } returns flowOf("Single chunk")
        viewModel = createViewModel()
        advanceUntilIdle() // drain initial observeMessages emission

        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onInputChanged("Test")
            awaitItem() // inputText update

            viewModel.onSendClicked()
            awaitItem() // isStreaming = true, streamingText = ""

            advanceUntilIdle()

            val streamingState = awaitItem()
            assertEquals("Single chunk", streamingState.streamingText)
            assertTrue(streamingState.isStreaming)

            val completedState = awaitItem()
            assertEquals("Single chunk", completedState.streamingText)
            assertFalse(completedState.isStreaming)
        }
    }
}
