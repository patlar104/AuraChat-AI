package com.aurachat.presentation.history

import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.usecase.DeleteSessionUseCase
import com.aurachat.domain.usecase.GetSessionsUseCase
import com.aurachat.testutil.MainDispatcherExtension
import com.aurachat.testutil.SharedFlowSubject
import com.aurachat.testutil.StateFlowSubject
import com.aurachat.testutil.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HistoryViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var getSessions: GetSessionsUseCase
    private lateinit var deleteSession: DeleteSessionUseCase
    private lateinit var sessionsFlow: SharedFlowSubject<List<ChatSession>>
    private lateinit var viewModel: HistoryViewModel

    private val session1 = ChatSession(
        id = 1L,
        title = "Session One",
        createdAt = 1_000L,
        updatedAt = 2_000L,
        messageCount = 3,
        lastMessagePreview = "Hello",
    )
    private val session2 = ChatSession(
        id = 2L,
        title = "Session Two",
        createdAt = 3_000L,
        updatedAt = 4_000L,
        messageCount = 1,
        lastMessagePreview = "World",
    )

    @BeforeEach
    fun setUp() {
        getSessions = mockk()
        deleteSession = mockk(relaxed = true)
        sessionsFlow = SharedFlowSubject()
        every { getSessions() } returns sessionsFlow.flow
    }

    private fun createViewModel(): HistoryViewModel {
        viewModel = HistoryViewModel(getSessions, deleteSession)
        return viewModel
    }

    @Test
    fun `initial state stays loading until the first sessions emission arrives`() = mainDispatcher.runTest {
        createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<ChatSession>(), viewModel.uiState.value.sessions)
    }

    @Test
    fun `sessions flow updates the visible history list`() = mainDispatcher.runTest {
        createViewModel()
        mainDispatcher.scheduler.runCurrent()

        sessionsFlow.tryEmit(listOf(session1, session2))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(session1, session2), viewModel.uiState.value.sessions)
    }

    @Test
    fun `flow failure exits loading state cleanly`() = mainDispatcher.runTest {
        every { getSessions() } returns flow { throw IllegalStateException("DB error") }
        createViewModel()

        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<ChatSession>(), viewModel.uiState.value.sessions)
    }

    @Test
    fun `delete request delegates once and later flow emissions update the list`() = mainDispatcher.runTest {
        val statefulSessions = StateFlowSubject(listOf(session1, session2))
        every { getSessions() } returns statefulSessions.flow
        coEvery { deleteSession.invoke(session1.id) } returns Unit
        createViewModel()

        mainDispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteSession(session1.id)
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { deleteSession.invoke(session1.id) }

        statefulSessions.value = listOf(session2)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(session2), viewModel.uiState.value.sessions)
    }
}
