package com.aurachat.domain.usecase

import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
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
class SaveMessageUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: SaveMessageUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = SaveMessageUseCase(repository)
    }

    @Test
    fun `invoke saves user message and returns generated ID`() = runTest {
        // Given: Repository returns generated ID
        val expectedId = 123L
        val message = ChatMessage(
            sessionId = 1L,
            content = "Hello, AI!",
            role = MessageRole.USER,
            timestamp = 1000L
        )
        coEvery { repository.saveMessage(message) } returns expectedId

        // When: Invoke use case
        val result = useCase(message)

        // Then: Should save message and return ID
        assertEquals(expectedId, result)
        coVerify { repository.saveMessage(message) }
    }

    @Test
    fun `invoke saves model message and returns generated ID`() = runTest {
        // Given: Repository returns generated ID
        val expectedId = 456L
        val message = ChatMessage(
            sessionId = 2L,
            content = "Hello, human!",
            role = MessageRole.MODEL,
            timestamp = 2000L
        )
        coEvery { repository.saveMessage(message) } returns expectedId

        // When: Invoke use case
        val result = useCase(message)

        // Then: Should save message and return ID
        assertEquals(expectedId, result)
        coVerify { repository.saveMessage(message) }
    }

    @Test
    fun `invoke saves message with empty content`() = runTest {
        // Given: Message with empty content
        val expectedId = 789L
        val message = ChatMessage(
            sessionId = 3L,
            content = "",
            role = MessageRole.USER,
            timestamp = 3000L
        )
        coEvery { repository.saveMessage(message) } returns expectedId

        // When: Invoke use case
        val result = useCase(message)

        // Then: Should save empty message and return ID
        assertEquals(expectedId, result)
        coVerify { repository.saveMessage(message) }
    }

    @Test
    fun `invoke saves message with long content`() = runTest {
        // Given: Message with long content
        val expectedId = 999L
        val longContent = "This is a very long message content that contains multiple paragraphs and exceeds typical message length to test that the use case handles long content correctly without modification."
        val message = ChatMessage(
            sessionId = 4L,
            content = longContent,
            role = MessageRole.USER,
            timestamp = 4000L
        )
        coEvery { repository.saveMessage(message) } returns expectedId

        // When: Invoke use case
        val result = useCase(message)

        // Then: Should save long message and return ID
        assertEquals(expectedId, result)
        coVerify { repository.saveMessage(message) }
    }

    @Test
    fun `invoke saves message with error flag`() = runTest {
        // Given: Message with error flag
        val expectedId = 111L
        val message = ChatMessage(
            sessionId = 5L,
            content = "Error occurred",
            role = MessageRole.MODEL,
            timestamp = 5000L,
            isError = true
        )
        coEvery { repository.saveMessage(message) } returns expectedId

        // When: Invoke use case
        val result = useCase(message)

        // Then: Should save error message and return ID
        assertEquals(expectedId, result)
        coVerify { repository.saveMessage(message) }
    }

    @Test
    fun `invoke saves message with streaming flag`() = runTest {
        // Given: Message with streaming flag
        val expectedId = 222L
        val message = ChatMessage(
            sessionId = 6L,
            content = "Streaming message",
            role = MessageRole.MODEL,
            timestamp = 6000L,
            isStreaming = true
        )
        coEvery { repository.saveMessage(message) } returns expectedId

        // When: Invoke use case
        val result = useCase(message)

        // Then: Should save streaming message and return ID
        assertEquals(expectedId, result)
        coVerify { repository.saveMessage(message) }
    }

    @Test
    fun `invoke saves multiple messages sequentially`() = runTest {
        // Given: Multiple messages
        val message1 = ChatMessage(
            sessionId = 7L,
            content = "First message",
            role = MessageRole.USER,
            timestamp = 7000L
        )
        val message2 = ChatMessage(
            sessionId = 7L,
            content = "Second message",
            role = MessageRole.MODEL,
            timestamp = 7001L
        )
        coEvery { repository.saveMessage(message1) } returns 100L
        coEvery { repository.saveMessage(message2) } returns 200L

        // When: Invoke use case multiple times
        val result1 = useCase(message1)
        val result2 = useCase(message2)

        // Then: Should save both messages and return respective IDs
        assertEquals(100L, result1)
        assertEquals(200L, result2)
        coVerify { repository.saveMessage(message1) }
        coVerify { repository.saveMessage(message2) }
    }

    @Test
    fun `invoke delegates to repository exactly once per call`() = runTest {
        // Given: Repository can save message
        val message = ChatMessage(
            sessionId = 8L,
            content = "Test message",
            role = MessageRole.USER,
            timestamp = 8000L
        )
        coEvery { repository.saveMessage(message) } returns 333L

        // When: Invoke use case
        useCase(message)

        // Then: Should call repository exactly once
        coVerify(exactly = 1) { repository.saveMessage(message) }
    }

    @Test
    fun `invoke preserves message properties without modification`() = runTest {
        // Given: Message with specific properties
        val message = ChatMessage(
            id = 99L,
            sessionId = 9L,
            content = "Preserved message",
            role = MessageRole.MODEL,
            timestamp = 9000L,
            isStreaming = false,
            isError = false
        )
        coEvery { repository.saveMessage(message) } returns 444L

        // When: Invoke use case
        useCase(message)

        // Then: Should pass message to repository without modification
        coVerify { repository.saveMessage(message) }
    }
}
