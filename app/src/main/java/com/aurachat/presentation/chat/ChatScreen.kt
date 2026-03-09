package com.aurachat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurachat.R
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import com.aurachat.ui.components.MessageBubble

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatContent(
        uiState = uiState,
        onInputChanged = viewModel::onInputChanged,
        onSendClicked = viewModel::onSendClicked,
        onRetryClicked = viewModel::onRetryClicked,
    )
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onRetryClicked: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Total visible items: DB messages + optional streaming bubble
    val totalItemCount = uiState.messages.size + if (uiState.streamingText != null) 1 else 0

    // Scroll to bottom when a new item appears (new DB message or streaming bubble starts)
    LaunchedEffect(totalItemCount) {
        if (totalItemCount > 0) {
            listState.animateScrollToItem(totalItemCount - 1)
        }
    }

    // Scroll to bottom as streaming chunks arrive (keeps growing bubble in view)
    LaunchedEffect(uiState.streamingText) {
        if (uiState.streamingText != null && totalItemCount > 0) {
            listState.animateScrollToItem(totalItemCount - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isLoadingMessages) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Persisted messages from Room — stable keys prevent scroll jumps on recomposition
            items(
                items = uiState.messages,
                key = { message -> message.id },
            ) { message ->
                MessageBubble(message = message)
            }

            // Live streaming bubble — synthesized from streamingText, not from DB
            uiState.streamingText?.let { text ->
                item(key = "streaming_bubble") {
                    MessageBubble(
                        message = ChatMessage(
                            id = -1L,
                            sessionId = -1L,
                            content = text,
                            role = MessageRole.MODEL,
                            timestamp = System.currentTimeMillis(),
                            isStreaming = true,
                        )
                    )
                }
            }

            // Inline error row shown below the last message
            uiState.errorMessageResId?.let { errorResId ->
                item(key = "error_row") {
                    ErrorRow(messageResId = errorResId, onRetry = onRetryClicked)
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
        )

        ChatInputBar(
            text = uiState.inputText,
            isStreaming = uiState.isStreaming,
            onTextChanged = onInputChanged,
            onSendClicked = onSendClicked,
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isStreaming: Boolean,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.chat_input_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            maxLines = 5,
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            enabled = !isStreaming,
        )

        if (isStreaming) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(
                onClick = onSendClicked,
                enabled = text.isNotBlank(),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = if (text.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(
    messageResId: Int,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.chat_retry),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
