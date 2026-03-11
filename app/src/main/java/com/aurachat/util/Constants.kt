package com.aurachat.util

/**
 * Application-wide constants for AuraChat.
 *
 * Centralizes magic numbers, default values, and configuration parameters to
 * improve maintainability and consistency across the codebase. Every numeric
 * or string literal that appears in more than one place, or whose meaning is
 * not immediately obvious, should live here.
 */
object Constants {

    /**
     * Session-related constants for chat session management.
     */
    object Session {
        /**
         * Maximum character length for auto-generated session titles.
         * Titles are truncated from the first user message to this length.
         */
        const val MAX_TITLE_LENGTH = 60

        /**
         * Maximum character length for the last-message preview shown in
         * the history drawer session list items.
         */
        const val MAX_PREVIEW_LENGTH = 80
    }

    /**
     * Gemini AI model configuration constants.
     */
    object Gemini {
        /**
         * Default Gemini model used when no user preference is stored.
         * gemini-2.0-flash retires June 1 2026; using 2.5-flash (stable, no -preview suffix).
         */
        const val DEFAULT_MODEL = "gemini-2.5-flash"

        /** Kept for backward compatibility — equals [DEFAULT_MODEL]. */
        const val MODEL_NAME = DEFAULT_MODEL

        /** Models available in the Settings model picker, ordered newest-first. */
        val AVAILABLE_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
        )

        /**
         * Maximum number of prior messages included in the chat history context
         * sent to Gemini per request. Caps token usage on long conversations.
         */
        const val HISTORY_LIMIT = 20

        /**
         * Sampling temperature controlling response randomness.
         * 0.0 = fully deterministic; 1.0 = maximally creative.
         */
        const val TEMPERATURE = 0.7f

        /**
         * Top-K sampling — limits token selection to the K most probable tokens
         * at each step to reduce low-quality outputs.
         */
        const val TOP_K = 40

        /**
         * Top-P (nucleus) sampling — considers only the smallest set of tokens
         * whose cumulative probability reaches P.
         */
        const val TOP_P = 0.95f

        /**
         * Maximum number of tokens the model can generate per response.
         */
        const val MAX_OUTPUT_TOKENS = 8192
    }

    /**
     * UI-related constants for Compose layout and styling.
     */
    object Ui {
        /**
         * Message bubble constants for chat UI.
         */
        object MessageBubble {
            /** Maximum width of a message bubble in dp. */
            const val MAX_WIDTH_DP = 280

            /** Standard corner radius for message bubbles in dp. */
            const val CORNER_RADIUS_DP = 20

            /** Small corner radius for the "tail" corner of message bubbles in dp. */
            const val TAIL_RADIUS_DP = 4

            /** Horizontal padding inside message bubbles in dp. */
            const val HORIZONTAL_PADDING_DP = 14

            /** Vertical padding inside message bubbles in dp. */
            const val VERTICAL_PADDING_DP = 10

            /** Spacing for timestamp text in dp. */
            const val TIMESTAMP_SPACING_DP = 2

            /** Horizontal offset for timestamp alignment in dp. */
            const val TIMESTAMP_OFFSET_DP = 4
        }

        /**
         * Animation constants for UI animations.
         */
        object Animation {
            /** Duration for streaming cursor blink animation in milliseconds. */
            const val CURSOR_BLINK_DURATION_MS = 500
        }
    }
}
