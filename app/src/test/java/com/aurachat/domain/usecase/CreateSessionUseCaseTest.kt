package com.aurachat.domain.usecase

import com.aurachat.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateSessionUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: CreateSessionUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = CreateSessionUseCase(repository)
    }

    @Test
    fun `invoke creates session with default title and returns generated ID`() = runTest {
        // Given: Repository returns generated ID
        val expectedId = 123L
        coEvery { repository.createSession(any()) } returns expectedId

        // When: Invoke use case without title
        val result = useCase()

        // Then: Should create session with default title and return ID
        assertEquals(expectedId, result)
        coVerify { repository.createSession("New Chat") }
    }

    @Test
    fun `invoke creates session with custom title and returns generated ID`() = runTest {
        // Given: Repository returns generated ID
        val expectedId = 456L
        val customTitle = "Custom Chat Title"
        coEvery { repository.createSession(customTitle) } returns expectedId

        // When: Invoke use case with custom title
        val result = useCase(customTitle)

        // Then: Should create session with custom title and return ID
        assertEquals(expectedId, result)
        coVerify { repository.createSession(customTitle) }
    }

    @Test
    fun `invoke creates session with empty title`() = runTest {
        // Given: Repository returns generated ID
        val expectedId = 789L
        val emptyTitle = ""
        coEvery { repository.createSession(emptyTitle) } returns expectedId

        // When: Invoke use case with empty title
        val result = useCase(emptyTitle)

        // Then: Should create session with empty title and return ID
        assertEquals(expectedId, result)
        coVerify { repository.createSession(emptyTitle) }
    }

    @Test
    fun `invoke creates session with long title`() = runTest {
        // Given: Repository returns generated ID
        val expectedId = 999L
        val longTitle = "This is a very long chat title that exceeds typical length expectations"
        coEvery { repository.createSession(longTitle) } returns expectedId

        // When: Invoke use case with long title
        val result = useCase(longTitle)

        // Then: Should create session with long title and return ID
        assertEquals(expectedId, result)
        coVerify { repository.createSession(longTitle) }
    }

    @Test
    fun `invoke delegates to repository exactly once`() = runTest {
        // Given: Repository returns generated ID
        coEvery { repository.createSession(any()) } returns 1L

        // When: Invoke use case
        useCase("Test Title")

        // Then: Should call repository exactly once
        coVerify(exactly = 1) { repository.createSession(any()) }
    }
}
