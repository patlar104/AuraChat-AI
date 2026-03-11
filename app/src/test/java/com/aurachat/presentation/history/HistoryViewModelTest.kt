package com.aurachat.presentation.history

import app.cash.turbine.test
import com.aurachat.domain.model.ChatSession
import com.aurachat.domain.usecase.DeleteSessionUseCase
import com.aurachat.domain.usecase.GetSessionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getSessions: GetSessionsUseCase
    private lateinit var deleteSession: DeleteSessionUseCase
    private lateinit var viewModel: HistoryViewModel

    private val session1 = ChatSession(
        id = 1L,
        title = "Session One",
        createdAt = 1000L,
        updatedAt = 2000L,
        messageCount = 3,
        lastMessagePreview = "Hello"
    )
    private val session2 = ChatSession(
        id = 2L,
        title = "Session Two",
        createdAt = 3000L,
        updatedAt = 4000L,
        messageCount = 1,
        lastMessagePreview = "World"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getSessions = mockk()
        deleteSession = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HistoryViewModel =
        HistoryViewModel(getSessions, deleteSession)

    @Test
    fun `initial state has isLoading true and empty sessions before flow emits`() = runTest {
        every { getSessions() } returns flowOf()

        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isLoading)
            assertEquals(emptyList<ChatSession>(), state.sessions)
        }
    }

    @Test
    fun `sessions are populated and isLoading becomes false when flow emits`() = runTest {
        every { getSessions() } returns flowOf(listOf(session1, session2))

        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial state

            advanceUntilIdle()

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(listOf(session1, session2), loaded.sessions)
        }
    }

    @Test
    fun `sessions update when flow emits a new list`() = runTest {
        val updatedList = listOf(session1)
        every { getSessions() } returns flow {
            emit(listOf(session1, session2))
            emit(updatedList)
        }

        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            advanceUntilIdle()

            val first = awaitItem()
            assertEquals(listOf(session1, session2), first.sessions)

            val second = awaitItem()
            assertEquals(updatedList, second.sessions)
        }
    }

    @Test
    fun `error in sessions flow sets isLoading false and keeps sessions empty`() = runTest {
        every { getSessions() } returns flow {
            throw RuntimeException("DB error")
        }

        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            advanceUntilIdle()

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(emptyList<ChatSession>(), errorState.sessions)
        }
    }

    @Test
    fun `deleteSession calls DeleteSessionUseCase with correct id`() = runTest {
        every { getSessions() } returns flowOf(listOf(session1))
        coEvery { deleteSession.invoke(any()) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteSession(session1.id)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteSession.invoke(session1.id) }
    }

    @Test
    fun `deleteSession with zero id still delegates to use case`() = runTest {
        every { getSessions() } returns flowOf(emptyList())
        coEvery { deleteSession.invoke(any()) } returns Unit

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteSession(0L)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteSession.invoke(0L) }
    }
}
