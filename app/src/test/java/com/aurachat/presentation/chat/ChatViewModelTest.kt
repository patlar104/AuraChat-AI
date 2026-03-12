package com.aurachat.presentation.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.usecase.ConsumePendingInitialPromptUseCase
import com.aurachat.domain.usecase.GetMessagesUseCase
import com.aurachat.domain.usecase.SendMessageUseCase
import com.aurachat.testutil.MainDispatcherExtension
import com.aurachat.testutil.SharedFlowSubject
import com.aurachat.testutil.assertStreamingFinished
import com.aurachat.testutil.runTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ChatViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val context: Context = mockk(relaxed = true)
    private val sessionId = 123L
    private val messageOne = ChatMessage(
        id = 1L,
        sessionId = sessionId,
        content = "Hello",
        role = MessageRole.USER,
        timestamp = 1_000L,
    )
    private val messageTwo = ChatMessage(
        id = 2L,
        sessionId = sessionId,
        content = "Hi there!",
        role = MessageRole.MODEL,
        timestamp = 2_000L,
    )

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var consumePendingInitialPromptUseCase: ConsumePendingInitialPromptUseCase
    private lateinit var getMessagesUseCase: GetMessagesUseCase
    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var messageUpdates: SharedFlowSubject<List<ChatMessage>>
    private lateinit var viewModel: ChatViewModel

    @BeforeEach
    fun setUp() {
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to sessionId))
        consumePendingInitialPromptUseCase = mockk()
        getMessagesUseCase = mockk()
        sendMessageUseCase = mockk()
        messageUpdates = SharedFlowSubject()

        coEvery { consumePendingInitialPromptUseCase(sessionId) } returns null
        every { getMessagesUseCase(sessionId) } returns messageUpdates.flow
    }

    private fun createViewModel(): ChatViewModel {
        viewModel = ChatViewModel(
            savedStateHandle = savedStateHandle,
            context = context,
            consumePendingInitialPromptUseCase = consumePendingInitialPromptUseCase,
            getMessages = getMessagesUseCase,
            sendMessage = sendMessageUseCase,
        )
        return viewModel
    }

    @Test
    fun `initial state stays loading before Room emits`() = mainDispatcher.runTest {
        createViewModel()

        assertEquals(sessionId, viewModel.sessionId)
        assertTrue(viewModel.uiState.value.isLoadingMessages)
        assertEquals(emptyList<ChatMessage>(), viewModel.uiState.value.messages)
        assertNull(viewModel.uiState.value.streamingText)
    }

    @Test
    fun `message observation updates the current room-backed state`() = mainDispatcher.runTest {
        createViewModel()
        mainDispatcher.scheduler.runCurrent()

        messageUpdates.tryEmit(listOf(messageOne, messageTwo))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingMessages)
        assertEquals(listOf(messageOne, messageTwo), viewModel.uiState.value.messages)
    }

    @Test
    fun `blank input is ignored`() = mainDispatcher.runTest {
        createViewModel()
        mainDispatcher.scheduler.runCurrent()

        viewModel.onInputChanged("   ")
        viewModel.onSendClicked()
        mainDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isStreaming)
        verify(exactly = 0) { sendMessageUseCase.invoke(any<Long>(), any<String>()) }
    }

    @Test
    fun `successful send accumulates streaming chunks in order and finishes cleanly`() = mainDispatcher.runTest {
        every { sendMessageUseCase.invoke(sessionId, "Test message") } returns flowOf("Hello", " there")
        createViewModel()
        mainDispatcher.scheduler.runCurrent()
        messageUpdates.tryEmit(emptyList())
        mainDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState
            .map { it.streamingText }
            .filterNotNull()
            .test {
                viewModel.onInputChanged("Test message")
                viewModel.onSendClicked()

                assertEquals("", awaitItem())
                assertEquals("Hello", awaitItem())
                assertEquals("Hello there", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.inputText)
        assertStreamingFinished(
            isStreaming = viewModel.uiState.value.isStreaming,
            streamingText = viewModel.uiState.value.streamingText,
            errorMessage = viewModel.uiState.value.errorMessage,
        )
        verify(exactly = 1) { sendMessageUseCase.invoke(sessionId, "Test message") }
    }

    @Test
    fun `room emissions preserve temporary streaming text while active and clear it on completion`() = mainDispatcher.runTest {
        val completion = CompletableDeferred<Unit>()
        val updatedMessages = listOf(messageOne, messageTwo)
        every { sendMessageUseCase.invoke(sessionId, "Test") } returns flow {
            emit("AI response")
            completion.await()
        }
        createViewModel()
        mainDispatcher.scheduler.runCurrent()
        messageUpdates.tryEmit(listOf(messageOne))
        mainDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Test")
        viewModel.onSendClicked()
        mainDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isStreaming)
        assertEquals("AI response", viewModel.uiState.value.streamingText)

        messageUpdates.tryEmit(updatedMessages)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(updatedMessages, viewModel.uiState.value.messages)
        assertEquals("AI response", viewModel.uiState.value.streamingText)
        assertTrue(viewModel.uiState.value.isStreaming)

        completion.complete(Unit)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(updatedMessages, viewModel.uiState.value.messages)
        assertFalse(viewModel.uiState.value.isStreaming)
        assertNull(viewModel.uiState.value.streamingText)
    }

    @Test
    fun `failed send restores prompt and retry metadata`() = mainDispatcher.runTest {
        every { sendMessageUseCase.invoke(sessionId, "Test message") } returns flow {
            throw RuntimeException("Network error")
        }
        createViewModel()
        mainDispatcher.scheduler.runCurrent()
        messageUpdates.tryEmit(emptyList())
        mainDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Test message")
        viewModel.onSendClicked()
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isStreaming)
        assertEquals("Test message", viewModel.uiState.value.inputText)
        assertEquals("Network error", viewModel.uiState.value.errorMessage)
        assertEquals("Test message", viewModel.uiState.value.lastFailedPrompt)
        assertNull(viewModel.uiState.value.lastFailedImageUri)
    }

    @Test
    fun `retry clears the error and re-runs the same prompt`() = mainDispatcher.runTest {
        var attempts = 0
        every { sendMessageUseCase.invoke(sessionId, "Retry me") } answers {
            if (attempts++ == 0) {
                flow { throw RuntimeException("First attempt failed") }
            } else {
                flowOf("Recovered")
            }
        }
        createViewModel()
        mainDispatcher.scheduler.runCurrent()
        messageUpdates.tryEmit(emptyList())
        mainDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("Retry me")
        viewModel.onSendClicked()
        mainDispatcher.scheduler.advanceUntilIdle()
        assertEquals("First attempt failed", viewModel.uiState.value.errorMessage)

        viewModel.onRetryClicked()
        mainDispatcher.scheduler.advanceUntilIdle()

        assertStreamingFinished(
            isStreaming = viewModel.uiState.value.isStreaming,
            streamingText = viewModel.uiState.value.streamingText,
            errorMessage = viewModel.uiState.value.errorMessage,
        )
        assertEquals("", viewModel.uiState.value.inputText)
        assertNull(viewModel.uiState.value.lastFailedPrompt)
        verify(exactly = 2) { sendMessageUseCase.invoke(sessionId, "Retry me") }
    }

    @Test
    fun `pending initial prompt auto-sends exactly once`() = mainDispatcher.runTest {
        coEvery { consumePendingInitialPromptUseCase(sessionId) } returns "Hello from home"
        every { sendMessageUseCase.invoke(sessionId, "Hello from home", null) } returns flowOf("Hi there!")

        createViewModel()
        mainDispatcher.scheduler.runCurrent()
        messageUpdates.tryEmit(emptyList())
        mainDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { sendMessageUseCase.invoke(sessionId, "Hello from home", null) }
        io.mockk.coVerify(exactly = 1) { consumePendingInitialPromptUseCase(sessionId) }
        assertEquals("", viewModel.uiState.value.inputText)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `image selection and clear update the pending attachment state`() = mainDispatcher.runTest {
        createViewModel()
        val imageUri = mockk<android.net.Uri>()

        viewModel.onImageSelected(imageUri)
        assertEquals(imageUri, viewModel.uiState.value.pendingImageUri)

        viewModel.onClearImage()
        assertNull(viewModel.uiState.value.pendingImageUri)
    }

    @Test
    fun `duplicate send is ignored while streaming is already active`() = mainDispatcher.runTest {
        val completion = CompletableDeferred<Unit>()
        every { sendMessageUseCase.invoke(sessionId, "First message") } returns flow {
            emit("Response")
            completion.await()
        }
        createViewModel()
        mainDispatcher.scheduler.runCurrent()
        messageUpdates.tryEmit(emptyList())
        mainDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged("First message")
        viewModel.onSendClicked()
        mainDispatcher.scheduler.runCurrent()

        viewModel.onInputChanged("Second message")
        viewModel.onSendClicked()
        mainDispatcher.scheduler.runCurrent()

        verify(exactly = 1) { sendMessageUseCase.invoke(sessionId, "First message") }
        completion.complete(Unit)
        mainDispatcher.scheduler.advanceUntilIdle()
    }
}
