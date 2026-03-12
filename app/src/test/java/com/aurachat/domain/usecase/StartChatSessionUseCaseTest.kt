package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import com.aurachat.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartChatSessionUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: StartChatSessionUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = StartChatSessionUseCase(repository)
    }

    @Test
    fun `invoke creates session with prompt-derived title and pending prompt`() = runTest {
        coEvery { repository.createSession(any(), any()) } returns 123L

        val result = useCase("Hello Aura")

        assertEquals(123L, result)
        coVerify {
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

        coVerify {
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

        coVerify {
            repository.createSession(
                title = longPrompt.take(Constants.Session.MAX_TITLE_LENGTH),
                pendingInitialPrompt = longPrompt,
            )
        }
    }

    @Test(expected = DomainError.ValidationError::class)
    fun `invoke throws validation error for blank prompt`() = runTest {
        useCase("   ")
    }
}
