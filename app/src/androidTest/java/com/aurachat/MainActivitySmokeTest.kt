package com.aurachat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.aurachat.testutil.AppStateSeeder
import com.aurachat.ui.TestTags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetAppState() = runBlocking {
        AppStateSeeder.resetAppState()
        composeRule.waitForIdle()
    }

    @Test
    fun appLaunches_toHome_andSettingsIsReachable() {
        composeRule.onNodeWithText(context.getString(R.string.home_greeting)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_open_settings))
            .performClick()

        composeRule.onNodeWithText(context.getString(R.string.settings_model_title))
            .assertIsDisplayed()
    }

    @Test
    fun drawerSeededSession_opensChat_andShowsPersistedMessage() {
        runBlocking {
            val sessionId = AppStateSeeder.seedSessionWithMessages(
                title = "Seeded smoke session",
                userMessage = "Summarize the latest test run in one sentence.",
                modelMessage = "All smoke coverage passed on the seeded path.",
            )
            composeRule.waitForIdle()

            composeRule.onNodeWithContentDescription(context.getString(R.string.cd_open_drawer))
                .performClick()
            composeRule.onNodeWithTag(TestTags.History.SESSION_ITEM_PREFIX + sessionId)
                .assertIsDisplayed()
                .performClick()

            composeRule.onNodeWithTag(TestTags.Chat.INPUT_FIELD)
                .assertIsDisplayed()
            composeRule.onNodeWithText("Summarize the latest test run in one sentence.")
                .assertIsDisplayed()
        }
    }

    @Test
    fun selectedModel_persists_across_recreation() {
        val updatedModel = "gemini-1.5-pro"

        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_open_settings))
            .performClick()
        composeRule.onNodeWithTag(TestTags.Settings.MODEL_ROW_PREFIX + updatedModel)
            .performClick()
        composeRule.onNodeWithTag(TestTags.Settings.MODEL_RADIO_PREFIX + updatedModel)
            .assertIsSelected()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithTag(TestTags.Settings.MODEL_RADIO_PREFIX + updatedModel)
            .assertIsSelected()
    }

    @Test
    fun repeatedSettingsNavigation_doesNotDuplicateDestination() {
        val settingsButton =
            composeRule.onNodeWithContentDescription(context.getString(R.string.cd_open_settings))

        settingsButton.performClick()
        composeRule.onNodeWithText(context.getString(R.string.settings_model_title))
            .assertIsDisplayed()
        settingsButton.performClick()

        pressBack()

        composeRule.onNodeWithText(context.getString(R.string.home_greeting))
            .assertIsDisplayed()
    }

    @Test
    fun reopeningSameChatSession_doesNotStackDuplicateDestinations() {
        runBlocking {
            val sessionId = AppStateSeeder.seedSessionWithMessages(
                title = "Single top session",
                userMessage = "Open this chat twice without duplicating it.",
                modelMessage = "SingleTop keeps one copy on the stack.",
            )
            composeRule.waitForIdle()

            openDrawerAndNavigateToSession(sessionId)
            composeRule.onNodeWithTag(TestTags.Chat.INPUT_FIELD)
                .assertIsDisplayed()
            composeRule.onNodeWithText("Open this chat twice without duplicating it.")
                .assertIsDisplayed()

            openDrawerAndNavigateToSession(sessionId)
            composeRule.onNodeWithTag(TestTags.Chat.INPUT_FIELD)
                .assertIsDisplayed()
            composeRule.onNodeWithText("Open this chat twice without duplicating it.")
                .assertIsDisplayed()

            pressBack()

            composeRule.onNodeWithText(context.getString(R.string.home_greeting))
                .assertIsDisplayed()
        }
    }

    private fun openDrawerAndNavigateToSession(sessionId: Long) {
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_open_drawer))
            .performClick()
        composeRule.onNodeWithTag(TestTags.History.SESSION_ITEM_PREFIX + sessionId)
            .assertIsDisplayed()
            .performClick()
    }
}
