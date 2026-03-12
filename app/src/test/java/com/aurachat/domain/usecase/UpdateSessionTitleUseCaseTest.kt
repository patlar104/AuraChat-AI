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
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class UpdateSessionTitleUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: UpdateSessionTitleUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = UpdateSessionTitleUseCase(repository)
    }

    @ParameterizedTest(name = "{index}: sessionId={0}, title=''{1}''")
    @MethodSource("validUpdates")
    fun `invoke updates valid session titles`(
        sessionId: Long,
        title: String,
    ) = runTest {
        coEvery { repository.updateSessionTitle(sessionId, title) } returns Unit

        useCase(sessionId, title)

        coVerify(exactly = 1) { repository.updateSessionTitle(sessionId, title) }
    }

    @ParameterizedTest(name = "{index}: title=''{0}''")
    @MethodSource("blankTitles")
    fun `invoke rejects blank titles`(title: String) = runTest {
        assertThrowsSuspend<DomainError.ValidationError> { useCase(123L, title) }
    }

    @ParameterizedTest(name = "{index}: sessionId={0}, title=''{1}''")
    @MethodSource("validUpdates")
    fun `invoke wraps unexpected repository errors`(
        sessionId: Long,
        title: String,
    ) = runTest {
        coEvery { repository.updateSessionTitle(sessionId, title) } throws IllegalStateException("boom")

        val error = assertThrowsSuspend<DomainError.DatabaseError> { useCase(sessionId, title) }

        assertEquals("Failed to update session title: boom", error.message)
    }

    private companion object {
        @JvmStatic
        fun validUpdates(): Stream<Arguments> = Stream.of(
            Arguments.of(123L, "Updated Chat Title"),
            Arguments.of(0L, "Title for zero ID"),
            Arguments.of(999L, "Chat with 🤖 AI & special chars: @#$%"),
        )

        @JvmStatic
        fun blankTitles(): Stream<Arguments> = Stream.of(
            Arguments.of(""),
            Arguments.of("   "),
        )
    }
}
