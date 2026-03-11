package com.aurachat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
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
