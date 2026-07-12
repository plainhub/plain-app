package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaVideoSizingTest {

    @Test
    fun `videoRatio returns 1 when height is zero`() {
        val sizing = MediaVideoSizing(
            containerSize = IntSize(1000, 500),
            videoIntrinsicSize = IntSize(1920, 0),
        )
        assertEquals(1f, sizing.videoRatio)
    }

    @Test
    fun `videoRatio computes width over height`() {
        val sizing = MediaVideoSizing(
            containerSize = IntSize(1000, 500),
            videoIntrinsicSize = IntSize(1920, 1080),
        )
        assertEquals(1920f / 1080f, sizing.videoRatio)
    }

    @Test
    fun `widthFixed is true when video wider than container`() {
        // container 2:1, video 16:9 (1.78 > 2.0? no, 1.78 < 2.0)
        // Let's use container 3:1 (ratio 3.0), video 16:9 (ratio 1.78) → widthFixed = false
        // Use container 1:1 (ratio 1.0), video 16:9 (ratio 1.78) → widthFixed = true
        val sizing = MediaVideoSizing(
            containerSize = IntSize(500, 500),
            videoIntrinsicSize = IntSize(1920, 1080),
        )
        assertTrue(sizing.widthFixed)
    }

    @Test
    fun `widthFixed is false when video taller than container`() {
        // container 16:9 (ratio 1.78), video 9:16 (ratio 0.5625) → widthFixed = false
        val sizing = MediaVideoSizing(
            containerSize = IntSize(1920, 1080),
            videoIntrinsicSize = IntSize(720, 1280),
        )
        assertFalse(sizing.widthFixed)
    }

    @Test
    fun `superSize is true when video exceeds container in both dimensions`() {
        val sizing = MediaVideoSizing(
            containerSize = IntSize(500, 300),
            videoIntrinsicSize = IntSize(1920, 1080),
        )
        assertTrue(sizing.superSize)
    }

    @Test
    fun `superSize is false when video fits within container`() {
        val sizing = MediaVideoSizing(
            containerSize = IntSize(1920, 1080),
            videoIntrinsicSize = IntSize(640, 480),
        )
        assertFalse(sizing.superSize)
    }

    @Test
    fun `superSize is false when video exceeds only one dimension`() {
        // wider but shorter
        val sizing = MediaVideoSizing(
            containerSize = IntSize(500, 1080),
            videoIntrinsicSize = IntSize(1920, 720),
        )
        assertFalse(sizing.superSize)
    }

    @Test
    fun `displaySize returns container when video intrinsic is zero`() {
        val container = IntSize(800, 600)
        val sizing = MediaVideoSizing(
            containerSize = container,
            videoIntrinsicSize = IntSize.Zero,
        )
        assertEquals(container, sizing.displaySize)
    }

    @Test
    fun `displaySize returns container when container is zero`() {
        val sizing = MediaVideoSizing(
            containerSize = IntSize.Zero,
            videoIntrinsicSize = IntSize(1920, 1080),
        )
        assertEquals(IntSize.Zero, sizing.displaySize)
    }

    @Test
    fun `displaySize fits to width when widthFixed`() {
        // container 500x500 (ratio 1.0), video 1920x1080 (ratio 1.78) → widthFixed
        val sizing = MediaVideoSizing(
            containerSize = IntSize(500, 500),
            videoIntrinsicSize = IntSize(1920, 1080),
        )
        assertTrue(sizing.widthFixed)
        val display = sizing.displaySize
        assertEquals(500, display.width)
        // height = 500 / 1.78 ≈ 281
        assertEquals((500f / (1920f / 1080f)).toInt(), display.height)
    }

    @Test
    fun `displaySize fits to height when not widthFixed`() {
        // container 1920x1080 (ratio 1.78), video 720x1280 (ratio 0.5625) → not widthFixed
        val sizing = MediaVideoSizing(
            containerSize = IntSize(1920, 1080),
            videoIntrinsicSize = IntSize(720, 1280),
        )
        assertFalse(sizing.widthFixed)
        val display = sizing.displaySize
        assertEquals(1080, display.height)
        // width = 1080 * 0.5625 = 607
        assertEquals((1080f * (720f / 1280f)).toInt(), display.width)
    }

    @Test
    fun `renderedSize applies scale to displaySize`() {
        val sizing = MediaVideoSizing(
            containerSize = IntSize(500, 500),
            videoIntrinsicSize = IntSize(1920, 1080),
        )
        val display = sizing.displaySize
        val rendered = sizing.renderedSize(2f)
        assertEquals((display.width * 2f).toInt(), rendered.width)
        assertEquals((display.height * 2f).toInt(), rendered.height)
    }

    @Test
    fun `maxScale uses intrinsic width for superSize`() {
        val sizing = MediaVideoSizing(
            containerSize = IntSize(500, 300),
            videoIntrinsicSize = IntSize(1920, 1080),
        )
        assertTrue(sizing.superSize)
        val display = sizing.displaySize
        assertEquals(1920f / display.width.toFloat(), sizing.maxScale)
    }

    @Test
    fun `maxScale uses container height when widthFixed but not superSize`() {
        // container 1000x1000, video 800x600 (ratio 1.33 > 1.0 → widthFixed, 600 < 1000 → not superSize)
        val sizing = MediaVideoSizing(
            containerSize = IntSize(1000, 1000),
            videoIntrinsicSize = IntSize(800, 600),
        )
        assertTrue(sizing.widthFixed)
        assertFalse(sizing.superSize)
        val display = sizing.displaySize
        assertEquals(1000f / display.height.toFloat(), sizing.maxScale)
    }

    @Test
    fun `maxScale uses container width when not widthFixed and not superSize`() {
        // container 1000x1000, video 600x800 (ratio 0.75 < 1.0 → not widthFixed, 800 < 1000 → not superSize)
        val sizing = MediaVideoSizing(
            containerSize = IntSize(1000, 1000),
            videoIntrinsicSize = IntSize(600, 800),
        )
        assertFalse(sizing.widthFixed)
        assertFalse(sizing.superSize)
        val display = sizing.displaySize
        assertEquals(1000f / display.width.toFloat(), sizing.maxScale)
    }
}
