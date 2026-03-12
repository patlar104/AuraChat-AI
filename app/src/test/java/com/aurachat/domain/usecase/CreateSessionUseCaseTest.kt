package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.repository.ChatRepository
import com.aurachat.testutil.assertThrowsSuspend
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class CreateSessionUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: CreateSessionUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = CreateSessionUseCase(repository)
    }

    @ParameterizedTest(name = "{index}: title=''{0}''")
    @MethodSource("validTitles")
    fun `invoke creates session with provided title`(
        title: String,
        expectedId: Long,
    ) = runTest {
        coEvery { repository.createSession(title, null) } returns expectedId

        assertEquals(expectedId, useCase(title))

        coVerify(exactly = 1) { repository.createSession(title, null) }
    }

    @Test
    fun `invoke uses default title when no title is provided`() = runTest {
        coEvery { repository.createSession("New Chat", null) } returns 123L

        assertEquals(123L, useCase())

        coVerify(exactly = 1) { repository.createSession("New Chat", null) }
    }

    @Test
    fun `invoke throws validation error for blank title`() = runTest {
        assertThrowsSuspend<DomainError.ValidationError> { useCase("   ") }
    }

    @Test
    fun `invoke wraps unexpected repository errors`() = runTest {
        coEvery { repository.createSession(any(), any()) } throws IllegalStateException("boom")

        val error = assertThrowsSuspend<DomainError.DatabaseError> { useCase("Test Title") }

        assertEquals("Failed to create session: boom", error.message)
    }

    private companion object {
        @JvmStatic
        fun validTitles(): Stream<Arguments> = Stream.of(
            Arguments.of("Custom Chat Title", 456L),
            Arguments.of("This is a very long chat title that exceeds typical length expectations", 999L),
        )
    }
}
