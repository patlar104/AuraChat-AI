package com.aurachat.presentation.chat

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aurachat.R
import com.aurachat.ui.TestTags
import com.aurachat.ui.theme.AuraChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typingIndicator_is_visible_while_streaming_has_started_without_text() {
        composeRule.setContent {
            AuraChatTheme {
                ChatScreenContent(
                    uiState = ChatUiState(
                        isStreaming = true,
                        streamingText = "",
                    ),
                    onInputChanged = { },
                    onSendClicked = { },
                    onRetryClicked = { },
                    onAttachClicked = { },
                    onClearImage = { },
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.Chat.TYPING_INDICATOR).assertIsDisplayed()
    }

    @Test
    fun retryRow_invokes_retry_callback_when_error_is_visible() {
        var retryCount = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AuraChatTheme {
                ChatScreenContent(
                    uiState = ChatUiState(errorMessage = "Something went wrong"),
                    onInputChanged = { },
                    onSendClicked = { },
                    onRetryClicked = { retryCount++ },
                    onAttachClicked = { },
                    onClearImage = { },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.chat_retry)).performClick()

        composeRule.runOnIdle {
            assertEquals(1, retryCount)
        }
    }

    @Test
    fun pendingImagePreview_renders_and_can_be_cleared() {
        var clearCount = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AuraChatTheme {
                ChatScreenContent(
                    uiState = ChatUiState(pendingImageUri = Uri.parse("content://aurachat/image")),
                    onInputChanged = { },
                    onSendClicked = { },
                    onRetryClicked = { },
                    onAttachClicked = { },
                    onClearImage = { clearCount++ },
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.Chat.IMAGE_PREVIEW_STRIP).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_clear_image))
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, clearCount)
        }
    }
}
