package com.aurachat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Background color for inline code spans and code blocks
private val CodeBackground = Color(0xFF2A2A2A)

/**
 * Renders a Gemini AI response with basic markdown formatting.
 *
 * Supported syntax:
 * - ` ```code block``` ` — monospace box with horizontal scroll
 * - `**bold**` — bold weight
 * - `*italic*` — italic style
 * - `` `inline code` `` — monospace with dark background
 * - `- item` or `* item` — bullet list
 * - Blank lines — spacing between paragraphs
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        splitByCodeBlocks(text).forEach { segment ->
            when (segment) {
                is Segment.TextBlock -> InlineMarkdownBlock(segment.content)
                is Segment.CodeBlock -> CodeBlockView(segment.content)
            }
        }
    }
}

// ── Code block view ────────────────────────────────────────────────────────────

@Composable
private fun CodeBlockView(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CodeBackground, shape = MaterialTheme.shapes.small)
            .padding(10.dp),
    ) {
        SelectionContainer {
            Text(
                text = code.trimEnd('\n'),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

// ── Inline markdown (non-code segments) ───────────────────────────────────────

@Composable
private fun InlineMarkdownBlock(text: String) {
    val lines = text.split('\n')
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                isBullet(line) -> BulletItem(line.trimStart().removePrefix("- ").removePrefix("* "))
                else -> InlineText(parseInline(line))
            }
            i++
        }
    }
}

@Composable
private fun BulletItem(content: String) {
    Row {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = parseInline(content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun InlineText(annotated: AnnotatedString) {
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

// ── Segmentation ───────────────────────────────────────────────────────────────

private sealed interface Segment {
    data class TextBlock(val content: String) : Segment
    data class CodeBlock(val content: String) : Segment
}

private val fencedCodeRegex = Regex("```(?:\\w+)?\\n?([\\s\\S]*?)```", RegexOption.MULTILINE)

private fun splitByCodeBlocks(text: String): List<Segment> {
    val segments = mutableListOf<Segment>()
    var cursor = 0
    for (match in fencedCodeRegex.findAll(text)) {
        if (match.range.first > cursor) {
            segments.add(Segment.TextBlock(text.substring(cursor, match.range.first)))
        }
        segments.add(Segment.CodeBlock(match.groupValues[1]))
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        segments.add(Segment.TextBlock(text.substring(cursor)))
    }
    return segments.ifEmpty { listOf(Segment.TextBlock(text)) }
}

// ── Inline parser (bold / italic / inline-code) ────────────────────────────────

// Matches **bold**, *italic*, `code` — in that precedence order
private val inlineRegex = Regex("""(\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`)""")

private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in inlineRegex.findAll(text)) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        val bold = match.groupValues[2]
        val italic = match.groupValues[3]
        val code = match.groupValues[4]
        when {
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
            code.isNotEmpty() -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    background = CodeBackground,
                )
            ) { append(code) }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

private fun isBullet(line: String): Boolean {
    val trimmed = line.trimStart()
    return trimmed.startsWith("- ") || trimmed.startsWith("* ")
}
