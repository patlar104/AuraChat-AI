package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import com.aurachat.testutil.assertThrowsSuspend
import com.aurachat.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StartChatSessionUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: StartChatSessionUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = StartChatSessionUseCase(repository)
    }

    @Test
    fun `invoke creates session with prompt-derived title and pending prompt`() = runTest {
        coEvery { repository.createSession(any(), any()) } returns 123L

        assertEquals(123L, useCase("Hello Aura"))

        coVerify(exactly = 1) {
            repository.createSession(
                title = "Hello Aura",
                pendingInitialPrompt = "Hello Aura",
            )
        }
    }

    @Test
    fun `invoke trims prompt before persisting`() = runTest {
        coEvery { repository.createSession(any(), any()) } returns 456L

        useCase("  Hello Aura  ")

        coVerify(exactly = 1) {
            repository.createSession(
                title = "Hello Aura",
                pendingInitialPrompt = "Hello Aura",
            )
        }
    }

    @Test
    fun `invoke truncates only the title`() = runTest {
        val longPrompt = "a".repeat(Constants.Session.MAX_TITLE_LENGTH + 15)
        coEvery { repository.createSession(any(), any()) } returns 789L

        useCase(longPrompt)

        coVerify(exactly = 1) {
            repository.createSession(
                title = longPrompt.take(Constants.Session.MAX_TITLE_LENGTH),
                pendingInitialPrompt = longPrompt,
            )
        }
    }

    @Test
    fun `invoke throws validation error for blank prompt`() = runTest {
        assertThrowsSuspend<DomainError.ValidationError> { useCase("   ") }
    }
}
