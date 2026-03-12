package com.aurachat.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageAttachmentStoreTest {

    @Test
    fun calculateScaledDimensions_keepsOriginalSize_whenWithinLimit() {
        val result = ImageAttachmentStore.calculateScaledDimensions(
            width = 800,
            height = 600,
            maxEdgePx = 1024,
        )

        assertEquals(800 to 600, result)
    }

    @Test
    fun calculateScaledDimensions_scalesPortraitImage_usingLongestEdge() {
        val result = ImageAttachmentStore.calculateScaledDimensions(
            width = 1536,
            height = 2048,
            maxEdgePx = 1024,
        )

        assertEquals(768 to 1024, result)
    }

    @Test
    fun calculateScaledDimensions_scalesLandscapeImage_usingLongestEdge() {
        val result = ImageAttachmentStore.calculateScaledDimensions(
            width = 4032,
            height = 3024,
            maxEdgePx = 1024,
        )

        assertEquals(1024 to 768, result)
    }
}
