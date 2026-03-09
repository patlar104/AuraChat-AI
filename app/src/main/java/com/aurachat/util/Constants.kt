package com.aurachat.util

/**
 * Application-wide constants for AuraChat.
 *
 * This file centralizes magic numbers, default values, and configuration
 * parameters to improve maintainability and consistency across the codebase.
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
    }

    /**
     * UI-related constants for Compose layout and styling.
     */
    object Ui {
        /**
         * Message bubble constants for chat UI.
         */
        object MessageBubble {
            /**
             * Maximum width of a message bubble in dp.
             */
            const val MAX_WIDTH_DP = 280

            /**
             * Standard corner radius for message bubbles in dp.
             */
            const val CORNER_RADIUS_DP = 20

            /**
             * Small corner radius for the "tail" corner of message bubbles in dp.
             */
            const val TAIL_RADIUS_DP = 4

            /**
             * Horizontal padding inside message bubbles in dp.
             */
            const val HORIZONTAL_PADDING_DP = 14

            /**
             * Vertical padding inside message bubbles in dp.
             */
            const val VERTICAL_PADDING_DP = 10

            /**
             * Spacing for timestamp text in dp.
             */
            const val TIMESTAMP_SPACING_DP = 2

            /**
             * Horizontal offset for timestamp alignment in dp.
             */
            const val TIMESTAMP_OFFSET_DP = 4
        }

        /**
         * Animation constants for UI animations.
         */
        object Animation {
            /**
             * Duration for streaming cursor blink animation in milliseconds.
             */
            const val CURSOR_BLINK_DURATION_MS = 500
        }
    }
}
