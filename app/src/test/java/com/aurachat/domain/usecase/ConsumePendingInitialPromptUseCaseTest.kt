package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsumePendingInitialPromptUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: ConsumePendingInitialPromptUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ConsumePendingInitialPromptUseCase(repository)
    }

    @Test
    fun `invoke returns claimed prompt`() = runTest {
        coEvery { repository.consumePendingInitialPrompt(123L) } returns "Hello"

        val result = useCase(123L)

        assertEquals("Hello", result)
        coVerify(exactly = 1) { repository.consumePendingInitialPrompt(123L) }
    }

    @Test
    fun `invoke returns null when nothing is pending`() = runTest {
        coEvery { repository.consumePendingInitialPrompt(123L) } returns null

        assertNull(useCase(123L))
    }

    @Test(expected = DomainError.DatabaseError::class)
    fun `invoke wraps unexpected errors`() = runTest {
        coEvery { repository.consumePendingInitialPrompt(123L) } throws IllegalStateException("boom")

        useCase(123L)
    }
}
