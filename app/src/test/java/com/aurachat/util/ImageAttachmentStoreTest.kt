package com.aurachat.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ImageAttachmentStoreTest {

    @ParameterizedTest(name = "{index}: {0}x{1} -> {3}x{4}")
    @MethodSource("scaledDimensions")
    fun `calculateScaledDimensions keeps aspect ratio within max edge`(
        width: Int,
        height: Int,
        maxEdgePx: Int,
        expectedWidth: Int,
        expectedHeight: Int,
    ) {
        assertEquals(
            expectedWidth to expectedHeight,
            ImageAttachmentStore.calculateScaledDimensions(width, height, maxEdgePx),
        )
    }

    private companion object {
        @JvmStatic
        fun scaledDimensions(): Stream<Arguments> = Stream.of(
            Arguments.of(800, 600, 1024, 800, 600),
            Arguments.of(1536, 2048, 1024, 768, 1024),
            Arguments.of(4032, 3024, 1024, 1024, 768),
        )
    }
}
