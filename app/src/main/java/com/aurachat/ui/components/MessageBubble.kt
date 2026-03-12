package com.aurachat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aurachat.R
import com.aurachat.domain.model.ChatMessage
import com.aurachat.domain.model.MessageRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Chat bubble shapes — large radius everywhere except the "tail" corner
private val UserBubbleShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 4.dp,   // Tail toward bottom-right (user is right-aligned)
)
private val ModelBubbleShape = RoundedCornerShape(
    topStart = 4.dp,    // Tail toward top-left (model is left-aligned)
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 20.dp,
)

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    // Model bubbles expand to show markdown content; user bubbles cap at 280dp
    val bubbleModifier = if (isUser) {
        Modifier.widthIn(max = 280.dp)
    } else {
        Modifier.fillMaxWidth(0.92f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = bubbleModifier
                .background(
                    color = if (isUser)
                        MaterialTheme.colorScheme.primaryContainer   // #1A2D4D
                    else
                        MaterialTheme.colorScheme.surface,           // #0D0D0D
                    shape = if (isUser) UserBubbleShape else ModelBubbleShape,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            val hasImage = !message.imageUri.isNullOrBlank()

            // Streaming started but no content yet — show animated typing indicator
            if (!isUser && message.isStreaming && message.content.isEmpty() && !hasImage) {
                TypingIndicator()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasImage) {
                        MessageAttachment(imageUri = requireNotNull(message.imageUri))
                    }

                    if (message.content.isNotEmpty() || message.isStreaming) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (isUser || message.isError) {
                                // User messages and error text use plain Text — no markdown needed
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (message.isError)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onBackground,  // #E8EAED
                                )
                            } else {
                                // Model responses render markdown: bold, italic, code blocks, bullets
                                MarkdownText(
                                    text = message.content,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                            if (message.isStreaming) {
                                StreamingCursor()
                            }
                        }
                    }
                }
            }
        }

        // Timestamp — hidden while streaming (no final timestamp until persisted)
        if (!message.isStreaming) {
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,  // #9AA0A6
                modifier = Modifier.padding(
                    start = if (isUser) 0.dp else 4.dp,
                    end = if (isUser) 4.dp else 0.dp,
                    top = 2.dp,
                ),
            )
        }
    }
}

@Composable
private fun MessageAttachment(imageUri: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.cd_message_image),
                modifier = Modifier
                    .sizeIn(minWidth = 180.dp, maxWidth = 240.dp)
                    .heightIn(min = 132.dp, max = 220.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = stringResource(R.string.chat_image_attached),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

// ── Typing indicator ───────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_indicator")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_alpha_$index",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

// ── Streaming cursor ───────────────────────────────────────────────────────────

@Composable
private fun StreamingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursor_alpha",
    )
    Text(
        text = "▌",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.alpha(alpha),
    )
}

private val timestampFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun formatTimestamp(timestampMs: Long): String =
    timestampFormatter.format(Date(timestampMs))
