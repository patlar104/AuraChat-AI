package com.aurachat.domain.usecase

import app.cash.turbine.test
import com.aurachat.data.remote.GeminiDataSource
import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.repository.ChatRepository
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendMessageUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var geminiDataSource: GeminiDataSource
    private lateinit var updateSessionTitle: UpdateSessionTitleUseCase
    private lateinit var useCase: SendMessageUseCase

    private val sessionId = 123L
    private val prompt = "Hello, how are you?"
    private val existingMessages = listOf(
        ChatMessage(
            id = 1L,
            sessionId = sessionId,
            content = "Previous user message",
            role = MessageRole.USER,
            timestamp = 500L,
        ),
        ChatMessage(
            id = 2L,
            sessionId = sessionId,
            content = "Previous AI response",
            role = MessageRole.MODEL,
            timestamp = 600L,
        ),
    )

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        geminiDataSource = mockk()
        updateSessionTitle = mockk(relaxed = true)
        useCase = SendMessageUseCase(repository, geminiDataSource, updateSessionTitle)
    }

    @Test
    fun `invoke saves both messages, streams chunks in order, and auto-titles first exchange`() = runTest {
        val savedMessages = mutableListOf<ChatMessage>()
        every { repository.getMessagesFlow(sessionId) } returns flowOf(emptyList())
        coEvery { repository.saveMessage(capture(savedMessages)) } returnsMany listOf(1L, 2L)
        every { geminiDataSource.sendMessage(emptyList(), prompt, null) } returns flowOf("Hello", " there", "!")

        useCase(sessionId, prompt).test {
            assertEquals("Hello", awaitItem())
            assertEquals(" there", awaitItem())
            assertEquals("!", awaitItem())
            awaitComplete()
        }

        assertEquals(2, savedMessages.size)
        assertEquals(prompt, savedMessages.first().content)
        assertEquals(MessageRole.USER, savedMessages.first().role)
        assertEquals("Hello there!", savedMessages.last().content)
        assertEquals(MessageRole.MODEL, savedMessages.last().role)
        coVerify(exactly = 1) { updateSessionTitle(sessionId, prompt.take(60)) }
    }

    @Test
    fun `invoke snapshots history before persisting the current prompt`() = runTest {
        every { repository.getMessagesFlow(sessionId) } returns flowOf(existingMessages)
        every { geminiDataSource.sendMessage(existingMessages, prompt, null) } returns flowOf("Response")

        useCase(sessionId, prompt).test {
            assertEquals("Response", awaitItem())
            awaitComplete()
        }

        coVerify(ordering = Ordering.ORDERED) {
            repository.getMessagesFlow(sessionId)
            repository.saveMessage(any())
            geminiDataSource.sendMessage(existingMessages, prompt, null)
        }
        coVerify(exactly = 0) { updateSessionTitle(any(), any()) }
    }

    @Test
    fun `invoke stores empty model response when streaming completes without chunks`() = runTest {
        val savedMessages = mutableListOf<ChatMessage>()
        every { repository.getMessagesFlow(sessionId) } returns flowOf(emptyList())
        coEvery { repository.saveMessage(capture(savedMessages)) } returnsMany listOf(1L, 2L)
        every { geminiDataSource.sendMessage(emptyList(), prompt, null) } returns flowOf()

        useCase(sessionId, prompt).test {
            awaitComplete()
        }

        assertEquals("", savedMessages.last().content)
        assertEquals(MessageRole.MODEL, savedMessages.last().role)
    }

    @Test
    fun `invoke assembles long streaming responses exactly once`() = runTest {
        val chunks = (1..20).map { "chunk$it " }
        val savedMessages = mutableListOf<ChatMessage>()
        every { repository.getMessagesFlow(sessionId) } returns flowOf(emptyList())
        coEvery { repository.saveMessage(capture(savedMessages)) } returnsMany listOf(1L, 2L)
        every { geminiDataSource.sendMessage(emptyList(), prompt, null) } returns flowOf(*chunks.toTypedArray())

        useCase(sessionId, prompt).test {
            repeat(chunks.size) { assertTrue(awaitItem().startsWith("chunk")) }
            awaitComplete()
        }

        assertEquals(chunks.joinToString(""), savedMessages.last().content)
        coVerify(exactly = 2) { repository.saveMessage(any()) }
    }

    @Test
    fun `invoke wraps network failures from Gemini as domain network errors`() = runTest {
        every { repository.getMessagesFlow(sessionId) } returns flowOf(emptyList())
        every { geminiDataSource.sendMessage(any(), any(), any()) } returns flow {
            throw java.io.IOException("offline")
        }

        useCase(sessionId, prompt).test {
            val error = awaitError()
            assertInstanceOf(DomainError.NetworkError::class.java, error)
            assertEquals("Failed to connect to AI service: offline", error.message)
        }
    }

    @Test
    fun `invoke wraps unexpected repository failures as database errors`() = runTest {
        every { repository.getMessagesFlow(sessionId) } returns flowOf(emptyList())
        coEvery { repository.saveMessage(any()) } throws IllegalStateException("db failed")

        useCase(sessionId, prompt).test {
            val error = awaitError()
            assertInstanceOf(DomainError.DatabaseError::class.java, error)
            assertEquals("Failed to send message: db failed", error.message)
        }
    }
}
