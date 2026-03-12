package com.aurachat.testutil

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aurachat.di.AppGraphEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.util.Constants

object AppStateSeeder {
    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    private val entryPoint: AppGraphEntryPoint
        get() = EntryPointAccessors.fromApplication(appContext, AppGraphEntryPoint::class.java)

    suspend fun resetAppState() {
        entryPoint.database().clearAllTables()
        entryPoint.settingsRepository().setSelectedModel(Constants.Gemini.DEFAULT_MODEL)
    }

    suspend fun seedSessionWithMessages(
        title: String,
        userMessage: String,
        modelMessage: String,
    ): Long {
        val repository = entryPoint.chatRepository()
        val sessionId = repository.createSession(title = title, pendingInitialPrompt = null)
        repository.saveMessage(
            ChatMessage(
                sessionId = sessionId,
                content = userMessage,
                role = MessageRole.USER,
                timestamp = 10_000L,
            ),
        )
        repository.saveMessage(
            ChatMessage(
                sessionId = sessionId,
                content = modelMessage,
                role = MessageRole.MODEL,
                timestamp = 20_000L,
            ),
        )
        return sessionId
    }
}
