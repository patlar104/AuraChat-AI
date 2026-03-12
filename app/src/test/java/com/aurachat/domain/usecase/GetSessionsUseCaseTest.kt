package com.aurachat.domain.usecase

import app.cash.turbine.test
import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatSession
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

class GetSessionsUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: GetSessionsUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetSessionsUseCase(repository)
    }

    @Test
    fun `invoke emits repository sessions without modification`() = runTest {
        val sessions = listOf(
            ChatSession(
                id = 1L,
                title = "Session 1",
                createdAt = 1_000L,
                updatedAt = 1_500L,
                messageCount = 2,
                lastMessagePreview = "Hello",
            ),
            ChatSession(
                id = 2L,
                title = "Session 2",
                createdAt = 2_000L,
                updatedAt = 2_500L,
                messageCount = 1,
                lastMessagePreview = "World",
            ),
        )
        every { repository.getSessionsFlow() } returns flowOf(sessions)

        useCase().test {
            assertEquals(sessions, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) { repository.getSessionsFlow() }
    }

    @Test
    fun `invoke wraps unexpected repository errors`() = runTest {
        every { repository.getSessionsFlow() } returns flow {
            throw IllegalStateException("boom")
        }

        useCase().test {
            val error = awaitError()
            assertInstanceOf(DomainError.DatabaseError::class.java, error)
            assertEquals("Failed to get sessions: boom", error.message)
        }
    }
}
