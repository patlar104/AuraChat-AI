package com.aurachat.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.aurachat.data.local.database.AuraChatDatabase
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RoomChatRepositoryTest {

    private lateinit var database: AuraChatDatabase
    private lateinit var repository: RoomChatRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AuraChatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomChatRepository(
            database = database,
            sessionDao = database.chatSessionDao(),
            messageDao = database.chatMessageDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createSession_persistsPendingInitialPrompt_andConsumeClearsItAtomically() = runTest {
        val sessionId = repository.createSession(
            title = "Hello Aura",
            pendingInitialPrompt = "Hello Aura",
        )

        assertEquals("Hello Aura", repository.getSessionById(sessionId)?.pendingInitialPrompt)
        assertEquals("Hello Aura", repository.consumePendingInitialPrompt(sessionId))
        assertNull(repository.consumePendingInitialPrompt(sessionId))
        assertNull(repository.getSessionById(sessionId)?.pendingInitialPrompt)
    }

    @Test
    fun saveMessage_updates_session_metadata_preview_count_and_timestamp() = runTest {
        val sessionId = repository.createSession(title = "Preview", pendingInitialPrompt = null)
        val timestamp = 5_000L

        repository.saveMessage(
            ChatMessage(
                sessionId = sessionId,
                content = "Image summary",
                imageUri = "content://image/1",
                role = MessageRole.USER,
                timestamp = timestamp,
            ),
        )

        val session = requireNotNull(repository.getSessionById(sessionId))
        assertEquals(1, session.messageCount)
        assertEquals("Image: Image summary", session.lastMessagePreview)
        assertEquals(timestamp, session.updatedAt)
    }

    @Test
    fun getMessagesFlow_emits_saved_messages_and_deleteSession_cascades_them() = runTest {
        val sessionId = repository.createSession(title = "Flow", pendingInitialPrompt = null)

        repository.getMessagesFlow(sessionId).test {
            assertEquals(emptyList<ChatMessage>(), awaitItem())

            repository.saveMessage(
                ChatMessage(
                    sessionId = sessionId,
                    content = "Hello",
                    role = MessageRole.USER,
                    timestamp = 1_000L,
                ),
            )

            val savedMessages = awaitItem()
            assertEquals(1, savedMessages.size)
            assertEquals("Hello", savedMessages.single().content)

            repository.deleteSession(sessionId)
            assertEquals(emptyList<ChatMessage>(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sessionsFlow_orders_recently_updated_sessions_first() = runTest {
        val firstSessionId = repository.createSession(title = "First", pendingInitialPrompt = null)
        val secondSessionId = repository.createSession(title = "Second", pendingInitialPrompt = null)

        repository.getSessionsFlow().test {
            val initialOrder = awaitItem()
            assertEquals(listOf(secondSessionId, firstSessionId), initialOrder.map { it.id })
            val newestTimestamp = initialOrder.maxOf { it.updatedAt } + 1_000L

            repository.saveMessage(
                ChatMessage(
                    sessionId = firstSessionId,
                    content = "Newest message",
                    role = MessageRole.USER,
                    timestamp = newestTimestamp,
                ),
            )

            val reordered = awaitItem()
            assertEquals(firstSessionId, reordered.first().id)
            assertTrue(reordered.first().updatedAt >= reordered.last().updatedAt)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
