package com.aurachat.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.aurachat.R
import com.aurachat.ui.TestTags
import com.aurachat.ui.theme.AuraChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sendButton_enables_for_non_blank_input_and_triggers_send() {
        var uiState by mutableStateOf(HomeUiState())
        var sendCount = 0

        composeRule.setContent {
            AuraChatTheme {
                HomeScreenContent(
                    uiState = uiState,
                    onInputChanged = { uiState = uiState.copy(inputText = it) },
                    onSend = { sendCount++ },
                    onSuggestionTapped = { },
                    onNavigationConsumed = { uiState = uiState.copy(navigateToSessionId = null) },
                    onErrorDismissed = { uiState = uiState.copy(errorMessage = null) },
                    onNavigateToChat = { },
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.Home.SEND_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(TestTags.Home.INPUT_FIELD).performTextInput("Hello Aura")
        composeRule.onNodeWithTag(TestTags.Home.SEND_BUTTON).assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(1, sendCount)
            assertEquals("Hello Aura", uiState.inputText)
        }
    }

    @Test
    fun suggestionTap_invokes_the_suggestion_callback() {
        var tappedSuggestion: String? = null
        val suggestion = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.suggestion_quantum)

        composeRule.setContent {
            AuraChatTheme {
                HomeScreenContent(
                    uiState = HomeUiState(),
                    onInputChanged = { },
                    onSend = { },
                    onSuggestionTapped = { tappedSuggestion = it },
                    onNavigationConsumed = { },
                    onErrorDismissed = { },
                    onNavigateToChat = { },
                )
            }
        }

        composeRule.onNodeWithText(suggestion).performClick()

        composeRule.runOnIdle {
            assertEquals(suggestion, tappedSuggestion)
        }
    }

    @Test
    fun pendingNavigation_invokes_callback_once_and_clears_the_event() {
        var uiState by mutableStateOf(HomeUiState(navigateToSessionId = 99L))
        val navigatedIds = mutableListOf<Long>()

        composeRule.setContent {
            AuraChatTheme {
                HomeScreenContent(
                    uiState = uiState,
                    onInputChanged = { uiState = uiState.copy(inputText = it) },
                    onSend = { },
                    onSuggestionTapped = { },
                    onNavigationConsumed = { uiState = uiState.copy(navigateToSessionId = null) },
                    onErrorDismissed = { uiState = uiState.copy(errorMessage = null) },
                    onNavigateToChat = { navigatedIds += it },
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(listOf(99L), navigatedIds)
            assertNull(uiState.navigateToSessionId)
        }
    }
}
