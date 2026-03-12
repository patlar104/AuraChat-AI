package com.aurachat.domain.usecase

import app.cash.turbine.test
import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.repository.ChatRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetMessagesUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: GetMessagesUseCase

    private val sessionId = 123L
    private val messages = listOf(
        ChatMessage(
            id = 1L,
            sessionId = sessionId,
            content = "Hello, how are you?",
            role = MessageRole.USER,
            timestamp = 1_000L,
        ),
        ChatMessage(
            id = 2L,
            sessionId = sessionId,
            content = "I'm doing great, thanks!",
            role = MessageRole.MODEL,
            timestamp = 2_000L,
        ),
    )

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetMessagesUseCase(repository)
    }

    @Test
    fun `invoke emits repository messages without modification`() = runTest {
        every { repository.getMessagesFlow(sessionId) } returns flowOf(messages)

        useCase(sessionId).test {
            assertEquals(messages, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) { repository.getMessagesFlow(sessionId) }
    }

    @Test
    fun `invoke wraps unexpected repository errors`() = runTest {
        every { repository.getMessagesFlow(sessionId) } returns flow {
            throw IllegalStateException("boom")
        }

        useCase(sessionId).test {
            val error = awaitError()
            assertInstanceOf(DomainError.DatabaseError::class.java, error)
            assertEquals("Failed to get messages: boom", error.message)
        }
    }
}
