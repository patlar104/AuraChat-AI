package com.aurachat.domain.usecase

import com.aurachat.domain.error.DomainError
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
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

class SaveMessageUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: SaveMessageUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SaveMessageUseCase(repository)
    }

    @ParameterizedTest(name = "{index}: message={0}")
    @MethodSource("validMessages")
    fun `invoke saves valid messages without mutation`(
        message: ChatMessage,
        expectedId: Long,
    ) = runTest {
        coEvery { repository.saveMessage(message) } returns expectedId

        assertEquals(expectedId, useCase(message))

        coVerify(exactly = 1) { repository.saveMessage(message) }
    }

    @ParameterizedTest(name = "{index}: message={0}")
    @MethodSource("invalidMessages")
    fun `invoke rejects blank content`(message: ChatMessage) = runTest {
        assertThrowsSuspend<DomainError.ValidationError> { useCase(message) }
    }

    @ParameterizedTest(name = "{index}: message={0}")
    @MethodSource("validMessages")
    fun `invoke wraps unexpected repository errors`(
        message: ChatMessage,
        @Suppress("UNUSED_PARAMETER") expectedId: Long,
    ) = runTest {
        coEvery { repository.saveMessage(message) } throws IllegalStateException("boom")

        val error = assertThrowsSuspend<DomainError.DatabaseError> { useCase(message) }

        assertEquals("Failed to save message: boom", error.message)
    }

    private companion object {
        @JvmStatic
        fun validMessages(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ChatMessage(
                    sessionId = 1L,
                    content = "Hello, AI!",
                    role = MessageRole.USER,
                    timestamp = 1_000L,
                ),
                123L,
            ),
            Arguments.of(
                ChatMessage(
                    sessionId = 2L,
                    content = "Hello, human!",
                    role = MessageRole.MODEL,
                    timestamp = 2_000L,
                ),
                456L,
            ),
            Arguments.of(
                ChatMessage(
                    sessionId = 3L,
                    content = "Streaming message",
                    role = MessageRole.MODEL,
                    timestamp = 3_000L,
                    isStreaming = true,
                ),
                789L,
            ),
        )

        @JvmStatic
        fun invalidMessages(): Stream<Arguments> = Stream.of(
            Arguments.of(
                ChatMessage(
                    sessionId = 4L,
                    content = "",
                    role = MessageRole.USER,
                    timestamp = 4_000L,
                )
            ),
            Arguments.of(
                ChatMessage(
                    sessionId = 5L,
                    content = "   ",
                    role = MessageRole.USER,
                    timestamp = 5_000L,
                )
            ),
        )
    }
}
