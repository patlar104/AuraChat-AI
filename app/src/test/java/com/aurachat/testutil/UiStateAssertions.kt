package com.aurachat.testutil

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

fun assertStreamingFinished(
    isStreaming: Boolean,
    streamingText: String?,
    errorMessage: String?,
) {
    assertFalse(isStreaming)
    assertNull(streamingText)
    assertNull(errorMessage)
}

fun assertStreamingActive(
    isStreaming: Boolean,
    streamingText: String?,
) {
    assertTrue(isStreaming)
    assertTrue(streamingText != null)
}
