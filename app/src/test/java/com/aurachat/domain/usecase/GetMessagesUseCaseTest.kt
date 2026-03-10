package com.aurachat.domain.usecase

import app.cash.turbine.test
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.domain.repository.ChatRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetMessagesUseCaseTest {

    private lateinit var repository: ChatRepository
    private lateinit var useCase: GetMessagesUseCase

    private val testSessionId = 123L
    private val otherSessionId = 456L

    private val testMessages = listOf(
        ChatMessage(
            id = 1L,
            sessionId = testSessionId,
            content = "Hello, how are you?",
            role = MessageRole.USER,
            timestamp = 1000L
        ),
        ChatMessage(
            id = 2L,
            sessionId = testSessionId,
            content = "I'm doing great, thanks!",
            role = MessageRole.MODEL,
            timestamp = 2000L
        ),
        ChatMessage(
            id = 3L,
            sessionId = testSessionId,
            content = "What can you help me with?",
            role = MessageRole.USER,
            timestamp = 3000L
        )
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetMessagesUseCase(repository)
    }

    @Test
    fun `invoke returns flow of messages from repository`() = runTest {
        // Given: Repository returns flow of test messages
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(testMessages)

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: Should emit the messages
            val emittedMessages = awaitItem()
            assertEquals(testMessages, emittedMessages)
            awaitComplete()
        }

        // Then: Verify repository was called with correct sessionId
        verify(exactly = 1) { repository.getMessagesFlow(testSessionId) }
    }

    @Test
    fun `invoke with empty messages list returns empty flow`() = runTest {
        // Given: Repository returns empty list
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(emptyList())

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: Should emit empty list
            val emittedMessages = awaitItem()
            assertTrue(emittedMessages.isEmpty())
            awaitComplete()
        }

        // Then: Verify repository was called
        verify(exactly = 1) { repository.getMessagesFlow(testSessionId) }
    }

    @Test
    fun `invoke with single message returns single message flow`() = runTest {
        // Given: Repository returns single message
        val singleMessage = listOf(testMessages[0])
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(singleMessage)

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: Should emit single message
            val emittedMessages = awaitItem()
            assertEquals(1, emittedMessages.size)
            assertEquals(singleMessage[0], emittedMessages[0])
            awaitComplete()
        }

        // Then: Verify repository was called
        verify(exactly = 1) { repository.getMessagesFlow(testSessionId) }
    }

    @Test
    fun `invoke passes correct sessionId to repository`() = runTest {
        // Given: Repository returns different messages for different sessions
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(testMessages)
        every { repository.getMessagesFlow(otherSessionId) } returns flowOf(emptyList())

        // When: Invoke use case with first sessionId
        useCase(testSessionId).test {
            val emittedMessages = awaitItem()
            assertEquals(testMessages, emittedMessages)
            awaitComplete()
        }

        // Then: Verify repository was called with correct sessionId
        verify(exactly = 1) { repository.getMessagesFlow(testSessionId) }
        verify(exactly = 0) { repository.getMessagesFlow(otherSessionId) }
    }

    @Test
    fun `invoke with different sessionIds returns different flows`() = runTest {
        // Given: Repository returns different messages for different sessions
        val messagesForSession1 = listOf(testMessages[0])
        val messagesForSession2 = listOf(testMessages[1], testMessages[2])
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(messagesForSession1)
        every { repository.getMessagesFlow(otherSessionId) } returns flowOf(messagesForSession2)

        // When: Invoke use case with first sessionId
        useCase(testSessionId).test {
            val emittedMessages = awaitItem()
            assertEquals(messagesForSession1, emittedMessages)
            awaitComplete()
        }

        // When: Invoke use case with second sessionId
        useCase(otherSessionId).test {
            val emittedMessages = awaitItem()
            assertEquals(messagesForSession2, emittedMessages)
            awaitComplete()
        }

        // Then: Verify repository was called with both sessionIds
        verify(exactly = 1) { repository.getMessagesFlow(testSessionId) }
        verify(exactly = 1) { repository.getMessagesFlow(otherSessionId) }
    }

    @Test
    fun `invoke returns messages in correct order`() = runTest {
        // Given: Repository returns ordered messages
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(testMessages)

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: Messages should be in same order as returned from repository
            val emittedMessages = awaitItem()
            assertEquals(testMessages.size, emittedMessages.size)
            for (i in testMessages.indices) {
                assertEquals(testMessages[i].id, emittedMessages[i].id)
                assertEquals(testMessages[i].content, emittedMessages[i].content)
                assertEquals(testMessages[i].timestamp, emittedMessages[i].timestamp)
            }
            awaitComplete()
        }
    }

    @Test
    fun `invoke with large message list returns all messages`() = runTest {
        // Given: Repository returns large list of messages
        val largeMessageList = (1..100).map { index ->
            ChatMessage(
                id = index.toLong(),
                sessionId = testSessionId,
                content = "Message $index",
                role = if (index % 2 == 0) MessageRole.USER else MessageRole.MODEL,
                timestamp = index * 1000L
            )
        }
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(largeMessageList)

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: Should emit all messages
            val emittedMessages = awaitItem()
            assertEquals(100, emittedMessages.size)
            assertEquals(largeMessageList, emittedMessages)
            awaitComplete()
        }
    }

    @Test
    fun `invoke returns flow that completes successfully`() = runTest {
        // Given: Repository returns flow
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(testMessages)

        // When: Invoke use case
        useCase(testSessionId).test {
            awaitItem()
            // Then: Flow should complete without errors
            awaitComplete()
        }
    }

    @Test
    fun `invoke with messages containing different roles preserves roles`() = runTest {
        // Given: Messages with alternating roles
        val messagesWithRoles = listOf(
            ChatMessage(
                id = 1L,
                sessionId = testSessionId,
                content = "User message",
                role = MessageRole.USER,
                timestamp = 1000L
            ),
            ChatMessage(
                id = 2L,
                sessionId = testSessionId,
                content = "Model response",
                role = MessageRole.MODEL,
                timestamp = 2000L
            )
        )
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(messagesWithRoles)

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: Roles should be preserved
            val emittedMessages = awaitItem()
            assertEquals(MessageRole.USER, emittedMessages[0].role)
            assertEquals(MessageRole.MODEL, emittedMessages[1].role)
            awaitComplete()
        }
    }

    @Test
    fun `invoke preserves all message properties`() = runTest {
        // Given: Message with all properties set
        val messageWithProperties = ChatMessage(
            id = 42L,
            sessionId = testSessionId,
            content = "Test content",
            role = MessageRole.USER,
            timestamp = 12345L,
            isStreaming = false,
            isError = false
        )
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(listOf(messageWithProperties))

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: All properties should be preserved
            val emittedMessages = awaitItem()
            val message = emittedMessages[0]
            assertEquals(42L, message.id)
            assertEquals(testSessionId, message.sessionId)
            assertEquals("Test content", message.content)
            assertEquals(MessageRole.USER, message.role)
            assertEquals(12345L, message.timestamp)
            assertEquals(false, message.isStreaming)
            assertEquals(false, message.isError)
            awaitComplete()
        }
    }

    @Test
    fun `invoke can be called multiple times with same sessionId`() = runTest {
        // Given: Repository returns flow each time
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(testMessages)

        // When: Invoke use case multiple times
        repeat(3) {
            useCase(testSessionId).test {
                val emittedMessages = awaitItem()
                assertEquals(testMessages, emittedMessages)
                awaitComplete()
            }
        }

        // Then: Repository was called 3 times
        verify(exactly = 3) { repository.getMessagesFlow(testSessionId) }
    }

    @Test
    fun `invoke delegates directly to repository without transformation`() = runTest {
        // Given: Repository returns specific list
        val specificMessages = listOf(testMessages[1])
        every { repository.getMessagesFlow(testSessionId) } returns flowOf(specificMessages)

        // When: Invoke use case
        useCase(testSessionId).test {
            // Then: Should receive exact same list (no transformation)
            val emittedMessages = awaitItem()
            assertTrue(emittedMessages === specificMessages || emittedMessages == specificMessages)
            awaitComplete()
        }
    }
}
