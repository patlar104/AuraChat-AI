package com.aurachat.domain.usecase

import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.repository.ChatRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetSessionsUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: GetSessionsUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = GetSessionsUseCase(repository)
    }

    @Test
    fun `invoke returns flow of sessions from repository`() = runTest {
        // Given: Repository returns a flow of sessions
        val testSessions = listOf(
            ChatSession(
                id = 1L,
                title = "Session 1",
                createdAt = 1000L,
                updatedAt = 1000L,
                messageCount = 5,
                lastMessagePreview = "Hello"
            ),
            ChatSession(
                id = 2L,
                title = "Session 2",
                createdAt = 2000L,
                updatedAt = 2000L,
                messageCount = 3,
                lastMessagePreview = "World"
            )
        )
        every { repository.getSessionsFlow() } returns flowOf(testSessions)

        // When: Invoke use case
        val result = useCase().first()

        // Then: Should return sessions from repository
        assertEquals(testSessions, result)
        verify { repository.getSessionsFlow() }
    }

    @Test
    fun `invoke returns empty list when no sessions exist`() = runTest {
        // Given: Repository returns empty flow
        every { repository.getSessionsFlow() } returns flowOf(emptyList())

        // When: Invoke use case
        val result = useCase().first()

        // Then: Should return empty list
        assertEquals(emptyList<ChatSession>(), result)
        verify { repository.getSessionsFlow() }
    }

    @Test
    fun `invoke returns single session correctly`() = runTest {
        // Given: Repository returns single session
        val testSession = ChatSession(
            id = 1L,
            title = "Single Session",
            createdAt = 1000L,
            updatedAt = 1000L,
            messageCount = 0,
            lastMessagePreview = ""
        )
        every { repository.getSessionsFlow() } returns flowOf(listOf(testSession))

        // When: Invoke use case
        val result = useCase().first()

        // Then: Should return single session
        assertEquals(1, result.size)
        assertEquals(testSession, result[0])
        verify { repository.getSessionsFlow() }
    }

    @Test
    fun `invoke delegates to repository without modification`() = runTest {
        // Given: Repository returns flow
        val testSessions = listOf(
            ChatSession(
                id = 1L,
                title = "Test",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        every { repository.getSessionsFlow() } returns flowOf(testSessions)

        // When: Invoke use case
        val result = useCase().first()

        // Then: Should delegate to repository without modification
        assertEquals(testSessions, result)
        verify(exactly = 1) { repository.getSessionsFlow() }
    }
}
