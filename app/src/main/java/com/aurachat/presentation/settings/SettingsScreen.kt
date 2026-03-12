package com.aurachat.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurachat.BuildConfig
import com.aurachat.R
import com.aurachat.ui.TestTags
import com.aurachat.util.Constants

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val githubUrl = stringResource(R.string.settings_github_url)
    SettingsScreenContent(
        uiState = uiState,
        onSelectModel = viewModel::setSelectedModel,
        onOpenGithub = { uriHandler.openUri(githubUrl) },
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onSelectModel: (String) -> Unit,
    onOpenGithub: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ── Model section ──────────────────────────────────────────────────────

        Text(
            text = stringResource(R.string.settings_model_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.settings_model_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Constants.Gemini.AVAILABLE_MODELS.forEach { model ->
            ModelRadioRow(
                modelName = model,
                selected = uiState.selectedModel == model,
                onClick = { onSelectModel(model) },
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // ── About section ──────────────────────────────────────────────────────

        Text(
            text = stringResource(R.string.settings_about_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        TextButton(
            onClick = onOpenGithub,
            modifier = Modifier.testTag(TestTags.Settings.GITHUB_BUTTON),
        ) {
            Text(
                text = stringResource(R.string.settings_github),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ModelRadioRow(
    modelName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.Settings.MODEL_ROW_PREFIX + modelName)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.testTag(TestTags.Settings.MODEL_RADIO_PREFIX + modelName),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = modelName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
