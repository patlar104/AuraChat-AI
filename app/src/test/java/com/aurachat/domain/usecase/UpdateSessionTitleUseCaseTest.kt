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
class UpdateSessionTitleUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: UpdateSessionTitleUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = UpdateSessionTitleUseCase(repository)
    }

    @Test
    fun `invoke updates session title`() = runTest {
        // Given: Repository can update session title
        val sessionId = 123L
        val newTitle = "Updated Chat Title"
        coEvery { repository.updateSessionTitle(sessionId, newTitle) } returns Unit

        // When: Invoke use case
        useCase(sessionId, newTitle)

        // Then: Should call repository updateSessionTitle
        coVerify { repository.updateSessionTitle(sessionId, newTitle) }
    }

    @Test
    fun `invoke updates session with empty title`() = runTest {
        // Given: Empty title
        val sessionId = 456L
        val emptyTitle = ""
        coEvery { repository.updateSessionTitle(sessionId, emptyTitle) } returns Unit

        // When: Invoke use case
        useCase(sessionId, emptyTitle)

        // Then: Should call repository with empty title
        coVerify { repository.updateSessionTitle(sessionId, emptyTitle) }
    }

    @Test
    fun `invoke updates session with long title`() = runTest {
        // Given: Long title
        val sessionId = 789L
        val longTitle = "This is a very long session title that contains many characters and exceeds typical length"
        coEvery { repository.updateSessionTitle(sessionId, longTitle) } returns Unit

        // When: Invoke use case
        useCase(sessionId, longTitle)

        // Then: Should call repository with long title
        coVerify { repository.updateSessionTitle(sessionId, longTitle) }
    }

    @Test
    fun `invoke updates different session IDs correctly`() = runTest {
        // Given: Multiple session IDs and titles
        val sessionId1 = 111L
        val sessionId2 = 222L
        val title1 = "Title 1"
        val title2 = "Title 2"
        coEvery { repository.updateSessionTitle(any(), any()) } returns Unit

        // When: Invoke use case with different IDs and titles
        useCase(sessionId1, title1)
        useCase(sessionId2, title2)

        // Then: Should call repository for each update
        coVerify { repository.updateSessionTitle(sessionId1, title1) }
        coVerify { repository.updateSessionTitle(sessionId2, title2) }
    }

    @Test
    fun `invoke updates session with special characters in title`() = runTest {
        // Given: Title with special characters
        val sessionId = 999L
        val specialTitle = "Chat with 🤖 AI & special chars: @#$%"
        coEvery { repository.updateSessionTitle(sessionId, specialTitle) } returns Unit

        // When: Invoke use case
        useCase(sessionId, specialTitle)

        // Then: Should call repository with special characters
        coVerify { repository.updateSessionTitle(sessionId, specialTitle) }
    }

    @Test
    fun `invoke updates session title for session ID zero`() = runTest {
        // Given: Session ID is zero
        val sessionId = 0L
        val title = "Title for zero ID"
        coEvery { repository.updateSessionTitle(sessionId, title) } returns Unit

        // When: Invoke use case
        useCase(sessionId, title)

        // Then: Should call repository with ID zero
        coVerify { repository.updateSessionTitle(sessionId, title) }
    }

    @Test
    fun `invoke delegates to repository exactly once per call`() = runTest {
        // Given: Repository can update session title
        val sessionId = 654L
        val title = "Test Title"
        coEvery { repository.updateSessionTitle(sessionId, title) } returns Unit

        // When: Invoke use case
        useCase(sessionId, title)

        // Then: Should call repository exactly once
        coVerify(exactly = 1) { repository.updateSessionTitle(sessionId, title) }
    }
}
