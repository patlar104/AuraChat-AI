package com.aurachat.domain.usecase

import com.aurachat.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteSessionUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: DeleteSessionUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = DeleteSessionUseCase(repository)
    }

    @Test
    fun `invoke deletes session by ID`() = runTest {
        // Given: Repository can delete session
        val sessionId = 123L
        coEvery { repository.deleteSession(sessionId) } returns Unit

        // When: Invoke use case
        useCase(sessionId)

        // Then: Should call repository deleteSession
        coVerify { repository.deleteSession(sessionId) }
    }

    @Test
    fun `invoke deletes different session IDs correctly`() = runTest {
        // Given: Multiple session IDs
        val sessionId1 = 111L
        val sessionId2 = 222L
        val sessionId3 = 333L
        coEvery { repository.deleteSession(any()) } returns Unit

        // When: Invoke use case with different IDs
        useCase(sessionId1)
        useCase(sessionId2)
        useCase(sessionId3)

        // Then: Should call repository for each ID
        coVerify { repository.deleteSession(sessionId1) }
        coVerify { repository.deleteSession(sessionId2) }
        coVerify { repository.deleteSession(sessionId3) }
    }

    @Test
    fun `invoke deletes session with ID zero`() = runTest {
        // Given: Session ID is zero
        val sessionId = 0L
        coEvery { repository.deleteSession(sessionId) } returns Unit

        // When: Invoke use case
        useCase(sessionId)

        // Then: Should call repository with ID zero
        coVerify { repository.deleteSession(sessionId) }
    }

    @Test
    fun `invoke deletes session with negative ID`() = runTest {
        // Given: Session ID is negative
        val sessionId = -1L
        coEvery { repository.deleteSession(sessionId) } returns Unit

        // When: Invoke use case
        useCase(sessionId)

        // Then: Should call repository with negative ID (validation is repository's concern)
        coVerify { repository.deleteSession(sessionId) }
    }

    @Test
    fun `invoke delegates to repository exactly once per call`() = runTest {
        // Given: Repository can delete session
        val sessionId = 456L
        coEvery { repository.deleteSession(sessionId) } returns Unit

        // When: Invoke use case
        useCase(sessionId)

        // Then: Should call repository exactly once
        coVerify(exactly = 1) { repository.deleteSession(sessionId) }
    }
}
