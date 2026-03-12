package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import com.aurachat.testutil.assertThrowsSuspend
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConsumePendingInitialPromptUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: ConsumePendingInitialPromptUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = ConsumePendingInitialPromptUseCase(repository)
    }

    @Test
    fun `invoke returns claimed prompt`() = runTest {
        coEvery { repository.consumePendingInitialPrompt(123L) } returns "Hello"

        assertEquals("Hello", useCase(123L))

        coVerify(exactly = 1) { repository.consumePendingInitialPrompt(123L) }
    }

    @Test
    fun `invoke returns null when nothing is pending`() = runTest {
        coEvery { repository.consumePendingInitialPrompt(123L) } returns null

        assertNull(useCase(123L))
    }

    @Test
    fun `invoke wraps unexpected errors`() = runTest {
        coEvery { repository.consumePendingInitialPrompt(123L) } throws IllegalStateException("boom")

        val error = assertThrowsSuspend<DomainError.DatabaseError> { useCase(123L) }

        assertEquals("Failed to load startup prompt: boom", error.message)
    }
}
