package com.ismartcoding.plain.ui.media.viewer.previewer

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThumbnailAspectRatioTest {
    @Test
    fun unknownMetadataDoesNotReplaceTheKnownThumbnailAspectRatio() = runTest {
        val item = TransformItemState(scope = backgroundScope)
        item.updateIntrinsicSize(Size(900f, 300f))
        item.updateIntrinsicSize(Size.Zero)
        assertEquals(Size(900f, 300f), item.intrinsicSize)
    }

    @Test
    fun squareCellFitsLandscapeAndPortraitImagesWithoutStretching() = runTest {
        val item = TransformItemState(scope = backgroundScope, blockSize = IntSize(300, 300))
        val transform = TransformContentState(scope = backgroundScope)
        transform.itemState = item
        transform.containerSize = IntSize(1080, 2400)
        item.updateIntrinsicSize(Size(900f, 300f))
        assertEquals(Size(300f, 100f), transform.displayRatioSize)
        assertEquals(Size(1080f, 360f), transform.fitSize)
        item.updateIntrinsicSize(Size(300f, 900f))
        assertEquals(Size(300f, 900f), transform.displayRatioSize)
        assertEquals(Size(800f, 2400f), transform.fitSize)
    }
}
