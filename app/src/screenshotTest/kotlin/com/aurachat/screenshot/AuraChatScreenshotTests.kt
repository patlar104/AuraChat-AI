package com.aurachat.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.aurachat.presentation.chat.ChatScreenContent
import com.aurachat.presentation.history.DrawerContent
import com.aurachat.presentation.home.HomeScreenContent
import com.aurachat.presentation.settings.SettingsScreenContent
import com.aurachat.ui.theme.AuraChatTheme

private const val SCREEN_WIDTH = 411
private const val SCREEN_HEIGHT = 891
private const val AMOLED_BACKGROUND = 0xFF000000

@Composable
private fun ScreenshotHost(content: @Composable () -> Unit) {
    AuraChatTheme {
        content()
    }
}

@PreviewTest
@Preview(
    name = "Home default",
    locale = "en",
    fontScale = 1f,
    showBackground = true,
    backgroundColor = AMOLED_BACKGROUND,
    widthDp = SCREEN_WIDTH,
    heightDp = SCREEN_HEIGHT,
)
@Composable
fun HomeDefaultScreenshot() {
    ScreenshotHost {
        HomeScreenContent(
            uiState = ScreenshotFixtures.homeDefault,
            onInputChanged = { },
            onSend = { },
            onSuggestionTapped = { },
            onNavigationConsumed = { },
            onErrorDismissed = { },
            onNavigateToChat = { },
        )
    }
}

@PreviewTest
@Preview(
    name = "Chat populated",
    locale = "en",
    fontScale = 1f,
    showBackground = true,
    backgroundColor = AMOLED_BACKGROUND,
    widthDp = SCREEN_WIDTH,
    heightDp = SCREEN_HEIGHT,
)
@Composable
fun ChatPopulatedScreenshot() {
    ScreenshotHost {
        ChatScreenContent(
            uiState = ScreenshotFixtures.chatConversation,
            onInputChanged = { },
            onSendClicked = { },
            onRetryClicked = { },
            onAttachClicked = { },
            onClearImage = { },
        )
    }
}

@PreviewTest
@Preview(
    name = "Chat error",
    locale = "en",
    fontScale = 1f,
    showBackground = true,
    backgroundColor = AMOLED_BACKGROUND,
    widthDp = SCREEN_WIDTH,
    heightDp = SCREEN_HEIGHT,
)
@Composable
fun ChatErrorScreenshot() {
    ScreenshotHost {
        ChatScreenContent(
            uiState = ScreenshotFixtures.chatError,
            onInputChanged = { },
            onSendClicked = { },
            onRetryClicked = { },
            onAttachClicked = { },
            onClearImage = { },
        )
    }
}

@PreviewTest
@Preview(
    name = "Drawer empty",
    locale = "en",
    fontScale = 1f,
    showBackground = true,
    backgroundColor = AMOLED_BACKGROUND,
    widthDp = 320,
    heightDp = SCREEN_HEIGHT,
)
@Composable
fun DrawerEmptyScreenshot() {
    ScreenshotHost {
        DrawerContent(
            uiState = ScreenshotFixtures.historyEmpty,
            onNavigateToSession = { },
            onDeleteSession = { },
            formatUpdatedAt = ScreenshotFixtures::fixedRelativeTime,
        )
    }
}

@PreviewTest
@Preview(
    name = "Drawer populated",
    locale = "en",
    fontScale = 1f,
    showBackground = true,
    backgroundColor = AMOLED_BACKGROUND,
    widthDp = 320,
    heightDp = SCREEN_HEIGHT,
)
@Composable
fun DrawerPopulatedScreenshot() {
    ScreenshotHost {
        DrawerContent(
            uiState = ScreenshotFixtures.historyPopulated,
            onNavigateToSession = { },
            onDeleteSession = { },
            formatUpdatedAt = ScreenshotFixtures::fixedRelativeTime,
        )
    }
}

@PreviewTest
@Preview(
    name = "Settings default",
    locale = "en",
    fontScale = 1f,
    showBackground = true,
    backgroundColor = AMOLED_BACKGROUND,
    widthDp = SCREEN_WIDTH,
    heightDp = SCREEN_HEIGHT,
)
@Composable
fun SettingsDefaultScreenshot() {
    ScreenshotHost {
        SettingsScreenContent(
            uiState = ScreenshotFixtures.settingsDefault,
            onSelectModel = { },
            onOpenGithub = { },
        )
    }
}
