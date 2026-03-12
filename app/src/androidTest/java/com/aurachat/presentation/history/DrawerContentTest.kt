package com.aurachat.presentation.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.aurachat.domain.model.ChatSession
import com.aurachat.ui.TestTags
import com.aurachat.ui.theme.AuraChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DrawerContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val session = ChatSession(
        id = 1L,
        title = "Session One",
        createdAt = 1_000L,
        updatedAt = System.currentTimeMillis(),
        messageCount = 1,
        lastMessagePreview = "Hello",
    )

    @Test
    fun emptyState_is_shown_when_there_are_no_sessions() {
        composeRule.setContent {
            AuraChatTheme {
                DrawerContent(
                    uiState = HistoryUiState(sessions = emptyList(), isLoading = false),
                    onNavigateToSession = { },
                    onDeleteSession = { },
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.History.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun tapping_a_session_invokes_navigation_callback() {
        var navigatedSessionId: Long? = null

        composeRule.setContent {
            AuraChatTheme {
                DrawerContent(
                    uiState = HistoryUiState(sessions = listOf(session), isLoading = false),
                    onNavigateToSession = { navigatedSessionId = it },
                    onDeleteSession = { },
                )
            }
        }

        composeRule.onNodeWithText(session.title).performClick()

        composeRule.runOnIdle {
            assertEquals(session.id, navigatedSessionId)
        }
    }

    @Test
    fun swiping_left_deletes_the_session() {
        var deletedSessionId: Long? = null

        composeRule.setContent {
            AuraChatTheme {
                DrawerContent(
                    uiState = HistoryUiState(sessions = listOf(session), isLoading = false),
                    onNavigateToSession = { },
                    onDeleteSession = { deletedSessionId = it },
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.History.SESSION_ITEM_PREFIX + session.id)
            .performTouchInput { swipeLeft() }

        composeRule.waitUntil(timeoutMillis = 5_000) { deletedSessionId == session.id }
    }
}
