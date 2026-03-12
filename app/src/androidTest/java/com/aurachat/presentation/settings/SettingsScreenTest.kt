package com.aurachat.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.aurachat.ui.TestTags
import com.aurachat.ui.theme.AuraChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selecting_a_model_invokes_the_selection_callback() {
        var selectedModel: String? = null

        composeRule.setContent {
            AuraChatTheme {
                SettingsScreenContent(
                    uiState = SettingsUiState(selectedModel = "gemini-2.5-flash"),
                    onSelectModel = { selectedModel = it },
                    onOpenGithub = { },
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.Settings.MODEL_ROW_PREFIX + "gemini-1.5-pro")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals("gemini-1.5-pro", selectedModel)
        }
    }

    @Test
    fun github_button_invokes_the_open_link_callback() {
        var openCount = 0

        composeRule.setContent {
            AuraChatTheme {
                SettingsScreenContent(
                    uiState = SettingsUiState(),
                    onSelectModel = { },
                    onOpenGithub = { openCount++ },
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.Settings.GITHUB_BUTTON)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, openCount)
        }
    }
}
