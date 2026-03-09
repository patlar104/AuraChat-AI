package com.aurachat.domain.usecase

import app.cash.turbine.test
import com.aurachat.data.remote.GeminiDataSource
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SendMessageUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var geminiDataSource: GeminiDataSource
    private lateinit var updateSessionTitle: UpdateSessionTitleUseCase
    private lateinit var useCase: SendMessageUseCase

    private val testSessionId = 123L
    private val testUserPrompt = "Hello, how are you?"
    private val testTimestamp = 1000L

    private val existingMessages = listOf(
        ChatMessage(
            id = 1L,
            sessionId = testSessionId,
            content = "Previous user message",
            role = MessageRole.USER,
            timestamp = 500L
        ),
        ChatMessage(
            id = 2L,
            sessionId = testSessionId,
            content = "Previous AI response",
            role = MessageRole.MODEL,
            timestamp = 600L
        )
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        geminiDataSource = mockk()
        updateSessionTitle = mockk(relaxed = true)
        useCase = SendMessageUseCase(repository, geminiDataSource, updateSessionTitle)
    }

    @Test
    fun `invoke with first message - saves user message, streams response, saves AI response, auto-titles session`() = runTest {
        // Given: First message (empty history)
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Hello", " there", "!")

        val savedMessages = mutableListOf<ChatMessage>()
        coEvery { repository.saveMessage(capture(savedMessages)) } returnsMany listOf(1L, 2L)

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            // Then: Should emit all streaming chunks
            assertEquals("Hello", awaitItem())
            assertEquals(" there", awaitItem())
            assertEquals("!", awaitItem())
            awaitComplete()
        }

        // Then: Verify user message was saved
        val userMessage = savedMessages[0]
        assertEquals(testSessionId, userMessage.sessionId)
        assertEquals(testUserPrompt, userMessage.content)
        assertEquals(MessageRole.USER, userMessage.role)
        assertTrue(userMessage.timestamp > 0)

        // Then: Verify AI message was saved
        val aiMessage = savedMessages[1]
        assertEquals(testSessionId, aiMessage.sessionId)
        assertEquals("Hello there!", aiMessage.content)
        assertEquals(MessageRole.MODEL, aiMessage.role)
        assertTrue(aiMessage.timestamp > 0)

        // Then: Verify Gemini was called with empty history
        coVerify { geminiDataSource.sendMessage(emptyList(), testUserPrompt) }

        // Then: Verify session title was updated (first message)
        coVerify { updateSessionTitle(testSessionId, testUserPrompt.take(60)) }
    }

    @Test
    fun `invoke with subsequent message - saves messages, streams response, does not auto-title`() = runTest {
        // Given: Existing history
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(existingMessages)
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Response")

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            assertEquals("Response", awaitItem())
            awaitComplete()
        }

        // Then: Verify Gemini was called with existing history
        coVerify { geminiDataSource.sendMessage(existingMessages, testUserPrompt) }

        // Then: Verify session title was NOT updated (not first message)
        coVerify(exactly = 0) { updateSessionTitle(any(), any()) }
    }

    @Test
    fun `invoke snapshots history before saving user message`() = runTest {
        // Given: History returns initial list then updated list
        val initialHistory = listOf(existingMessages[0])
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(initialHistory)
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Response")

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            awaitItem()
            awaitComplete()
        }

        // Then: Verify Gemini was called with initial history (before user message was saved)
        coVerify { geminiDataSource.sendMessage(initialHistory, testUserPrompt) }

        // Then: Verify repository.saveMessage was called AFTER history snapshot
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            repository.getMessagesFlow(testSessionId)
            repository.saveMessage(any())
            geminiDataSource.sendMessage(any(), any())
        }
    }

    @Test
    fun `invoke emits multiple streaming chunks in order`() = runTest {
        // Given
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        val chunks = listOf("Chunk", " 1", ", Chunk", " 2", ", Chunk", " 3")
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf(*chunks.toTypedArray())

        // When: Invoke use case
        val emittedChunks = mutableListOf<String>()
        useCase(testSessionId, testUserPrompt).test {
            repeat(chunks.size) {
                emittedChunks.add(awaitItem())
            }
            awaitComplete()
        }

        // Then: All chunks emitted in order
        assertEquals(chunks, emittedChunks)

        // Then: Full response saved
        val savedMessages = mutableListOf<ChatMessage>()
        coVerify { repository.saveMessage(capture(savedMessages)) }

        val aiMessage = savedMessages.last() // Get the AI message (not user message)
        assertEquals("Chunk 1, Chunk 2, Chunk 3", aiMessage.content)
    }

    @Test
    fun `invoke with empty streaming response completes successfully`() = runTest {
        // Given: Empty stream from Gemini
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf()

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            awaitComplete()
        }

        // Then: Empty AI message saved
        val savedMessages = mutableListOf<ChatMessage>()
        coVerify { repository.saveMessage(capture(savedMessages)) }

        val aiMessage = savedMessages.last()
        assertEquals("", aiMessage.content)
        assertEquals(MessageRole.MODEL, aiMessage.role)
    }

    @Test
    fun `invoke with single chunk response`() = runTest {
        // Given: Single chunk response
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Single chunk response")

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            assertEquals("Single chunk response", awaitItem())
            awaitComplete()
        }

        // Then: Response saved correctly
        val savedMessages = mutableListOf<ChatMessage>()
        coVerify { repository.saveMessage(capture(savedMessages)) }

        val aiMessage = savedMessages.last()
        assertEquals("Single chunk response", aiMessage.content)
    }

    @Test
    fun `invoke truncates title to 60 characters on first message`() = runTest {
        // Given: First message with long prompt
        val longPrompt = "a".repeat(100)
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Response")

        // When: Invoke use case
        useCase(testSessionId, longPrompt).test {
            awaitItem()
            awaitComplete()
        }

        // Then: Title truncated to 60 characters
        coVerify { updateSessionTitle(testSessionId, longPrompt.take(60)) }
    }

    @Test
    fun `invoke saves both messages with correct roles and timestamps`() = runTest {
        // Given
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("AI response")

        val savedMessages = mutableListOf<ChatMessage>()
        coEvery { repository.saveMessage(capture(savedMessages)) } returnsMany listOf(1L, 2L)

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            awaitItem()
            awaitComplete()
        }

        // Then: Two messages saved
        assertEquals(2, savedMessages.size)

        // Then: User message saved first
        val userMessage = savedMessages[0]
        assertEquals(testUserPrompt, userMessage.content)
        assertEquals(MessageRole.USER, userMessage.role)
        assertEquals(testSessionId, userMessage.sessionId)
        assertTrue(userMessage.timestamp > 0)

        // Then: AI message saved second
        val aiMessage = savedMessages[1]
        assertEquals("AI response", aiMessage.content)
        assertEquals(MessageRole.MODEL, aiMessage.role)
        assertEquals(testSessionId, aiMessage.sessionId)
        assertTrue(aiMessage.timestamp > 0)
        assertTrue(aiMessage.timestamp >= userMessage.timestamp)
    }

    @Test
    fun `invoke passes correct history to Gemini excluding current prompt`() = runTest {
        // Given: History with multiple messages
        val history = listOf(
            ChatMessage(
                id = 1L,
                sessionId = testSessionId,
                content = "User msg 1",
                role = MessageRole.USER,
                timestamp = 1000L
            ),
            ChatMessage(
                id = 2L,
                sessionId = testSessionId,
                content = "AI response 1",
                role = MessageRole.MODEL,
                timestamp = 2000L
            ),
            ChatMessage(
                id = 3L,
                sessionId = testSessionId,
                content = "User msg 2",
                role = MessageRole.USER,
                timestamp = 3000L
            ),
            ChatMessage(
                id = 4L,
                sessionId = testSessionId,
                content = "AI response 2",
                role = MessageRole.MODEL,
                timestamp = 4000L
            )
        )
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(history)
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Response")

        // When: Invoke use case
        useCase(testSessionId, "New message").test {
            awaitItem()
            awaitComplete()
        }

        // Then: Gemini receives full history (excluding new prompt)
        coVerify { geminiDataSource.sendMessage(history, "New message") }
    }

    @Test
    fun `invoke with very long AI response assembles correctly`() = runTest {
        // Given: Many small chunks
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        val chunks = (1..50).map { "chunk$it " }
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf(*chunks.toTypedArray())

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            repeat(50) { awaitItem() }
            awaitComplete()
        }

        // Then: Full response assembled and saved
        val savedMessages = mutableListOf<ChatMessage>()
        coVerify { repository.saveMessage(capture(savedMessages)) }

        val aiMessage = savedMessages.last()
        val expectedResponse = chunks.joinToString("")
        assertEquals(expectedResponse, aiMessage.content)
    }

    @Test
    fun `invoke detects first message correctly when history is empty`() = runTest {
        // Given: Empty history
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Response")

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            awaitItem()
            awaitComplete()
        }

        // Then: Auto-title called
        coVerify(exactly = 1) { updateSessionTitle(testSessionId, any()) }
    }

    @Test
    fun `invoke detects non-first message correctly when history has messages`() = runTest {
        // Given: Non-empty history
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(listOf(existingMessages[0]))
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Response")

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            awaitItem()
            awaitComplete()
        }

        // Then: Auto-title NOT called
        coVerify(exactly = 0) { updateSessionTitle(any(), any()) }
    }

    @Test
    fun `invoke calls repository saveMessage exactly twice`() = runTest {
        // Given
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf("Response")

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            awaitItem()
            awaitComplete()
        }

        // Then: saveMessage called twice (user + AI)
        coVerify(exactly = 2) { repository.saveMessage(any()) }
    }

    @Test
    fun `invoke uses StringBuilder to accumulate streaming response`() = runTest {
        // Given: Multiple chunks
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())
        val chunks = listOf("Hello", " ", "world", "!")
        coEvery { geminiDataSource.sendMessage(any(), any()) } returns flowOf(*chunks.toTypedArray())

        // When: Invoke use case
        useCase(testSessionId, testUserPrompt).test {
            repeat(chunks.size) { awaitItem() }
            awaitComplete()
        }

        // Then: Full response assembled correctly
        val savedMessages = mutableListOf<ChatMessage>()
        coVerify { repository.saveMessage(capture(savedMessages)) }

        val aiMessage = savedMessages.last()
        assertEquals("Hello world!", aiMessage.content)
    }
}
