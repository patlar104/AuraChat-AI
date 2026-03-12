package com.aurachat.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aurachat.data.local.database.AuraChatDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomChatRepositoryTest {

    private lateinit var database: AuraChatDatabase
    private lateinit var repository: RoomChatRepository

    @Before
    fun setup() {
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
    fun createSession_persistsPendingInitialPrompt_andConsumeClearsItAtomically() = runBlocking {
        val sessionId = repository.createSession(
            title = "Hello Aura",
            pendingInitialPrompt = "Hello Aura",
        )

        assertEquals("Hello Aura", repository.getSessionById(sessionId)?.pendingInitialPrompt)
        assertEquals("Hello Aura", repository.consumePendingInitialPrompt(sessionId))
        assertNull(repository.consumePendingInitialPrompt(sessionId))
        assertNull(repository.getSessionById(sessionId)?.pendingInitialPrompt)
    }
}
