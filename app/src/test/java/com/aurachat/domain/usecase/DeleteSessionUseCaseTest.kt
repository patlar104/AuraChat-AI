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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DeleteSessionUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: DeleteSessionUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = DeleteSessionUseCase(repository)
    }

    @ParameterizedTest(name = "{index}: sessionId={0}")
    @ValueSource(longs = [123L, 0L, -1L])
    fun `invoke deletes the requested session id`(sessionId: Long) = runTest {
        coEvery { repository.deleteSession(sessionId) } returns Unit

        useCase(sessionId)

        coVerify(exactly = 1) { repository.deleteSession(sessionId) }
    }

    @ParameterizedTest(name = "{index}: sessionId={0}")
    @ValueSource(longs = [42L])
    fun `invoke wraps unexpected repository errors`(sessionId: Long) = runTest {
        coEvery { repository.deleteSession(sessionId) } throws IllegalStateException("boom")

        val error = assertThrowsSuspend<DomainError.DatabaseError> { useCase(sessionId) }

        assertEquals("Failed to delete session: boom", error.message)
    }
}
