package com.ismartcoding.plain.tests

import com.ismartcoding.plain.lib.screenmirror.ScreenMirrorCaptureSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenMirrorCaptureSizeTest {

    // ── compute ──────────────────────────────────────────────────────────────

    @Test
    fun `compute returns physical size when within all limits`() {
        // 1080x1920 screen, encoder supports up to 4096x4096, target 1080
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 1920, shortTarget = 1080,
            maxW = 4096, maxH = 4096, wAlign = 2, hAlign = 2,
        )
        assertEquals(1080, w)
        assertEquals(1920, h)
    }

    @Test
    fun `compute scales down when height exceeds encoder max`() {
        // Mi 9: 1080x2340 screen, OMX encoder max height = 2304
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2340, shortTarget = 1080,
            maxW = 4096, maxH = 2304, wAlign = 2, hAlign = 2,
        )
        assertTrue("height $h must not exceed encoder max 2304", h <= 2304)
        assertEquals(2304, h)
        assertTrue("width $w must be even", w % 2 == 0)
        assertTrue("width $w must not exceed physical 1080", w <= 1080)
    }

    @Test
    fun `compute scales down when width exceeds encoder max`() {
        // Landscape tablet: 2560x1440, encoder max width = 1920
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 2560, physH = 1440, shortTarget = 1080,
            maxW = 1920, maxH = 4096, wAlign = 2, hAlign = 2,
        )
        assertTrue("width $w must not exceed encoder max 1920", w <= 1920)
        assertTrue("height $h must not exceed physical 1440", h <= 1440)
    }

    @Test
    fun `compute honors short target smaller than physical`() {
        // 720 Smooth mode on a 1080x2340 screen
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2340, shortTarget = 720,
            maxW = 4096, maxH = 2304, wAlign = 2, hAlign = 2,
        )
        val shortSide = minOf(w, h)
        assertTrue("short side $shortSide must not exceed target 720", shortSide <= 720)
    }

    @Test
    fun `compute applies 16-alignment`() {
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2340, shortTarget = 1080,
            maxW = 4096, maxH = 2304, wAlign = 16, hAlign = 16,
        )
        assertEquals("width must be 16-aligned", 0, w % 16)
        assertEquals("height must be 16-aligned", 0, h % 16)
        assertTrue("height $h must not exceed encoder max 2304", h <= 2304)
    }

    @Test
    fun `compute Mi 9 scenario produces 1062x2304`() {
        // Exact Mi 9 reproduction: 1080x2340, encoder max 4096x2304, align 2
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2340, shortTarget = 1080,
            maxW = 4096, maxH = 2304, wAlign = 2, hAlign = 2,
        )
        assertEquals(1062, w)
        assertEquals(2304, h)
    }

    @Test
    fun `compute Pixel 9 scenario is unchanged`() {
        // Pixel 9: 1080x2424, Codec2 encoder supports up to 4096x4096
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2424, shortTarget = 1080,
            maxW = 4096, maxH = 4096, wAlign = 2, hAlign = 2,
        )
        assertEquals(1080, w)
        assertEquals(2424, h)
    }

    // ── maxPixels constraint ─────────────────────────────────────────────────

    @Test
    fun `maxPixels MAX_VALUE does not downscale Pixel 9`() {
        // Pixel 9 (1080×2424, ~2.62 Mpix) with no pixel cap → native resolution
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2424, shortTarget = 1080,
            maxW = 4096, maxH = 4096, wAlign = 2, hAlign = 2,
            maxPixels = Int.MAX_VALUE,
        )
        assertEquals(1080, w)
        assertEquals(2424, h)
    }

    @Test
    fun `maxPixels MAX_VALUE does not downscale 1080p`() {
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2400, shortTarget = 1080,
            maxW = 4096, maxH = 4096, wAlign = 2, hAlign = 2,
            maxPixels = Int.MAX_VALUE,
        )
        assertEquals(1080, w)
        assertEquals(2400, h)
    }

    @Test
    fun `maxPixels 1_5M downscales S9`() {
        // S9: 1080×2220, 1.5 Mpix cap
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 1080, physH = 2220, shortTarget = 1080,
            maxW = 4096, maxH = 4096, wAlign = 2, hAlign = 2,
            maxPixels = 1_500_000,
        )
        val pixels = w.toLong() * h.toLong()
        assertTrue("pixels $pixels must not exceed 1_500_000", pixels <= 1_500_000)
        assertTrue("width $w must be > 0", w > 0)
        assertTrue("height $h must be > 0", h > 0)
    }

    @Test
    fun `maxPixels 1_5M preserves reproduced landscape orientation`() {
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 2340, physH = 1080, shortTarget = 1080,
            maxW = 4096, maxH = 4096, wAlign = 2, hAlign = 2,
            maxPixels = 1_500_000,
        )
        assertEquals(1802, w)
        assertEquals(832, h)
        assertTrue("landscape width $w must exceed height $h", w > h)
    }

    @Test
    fun `compute with maxPixels higher than physical has no effect`() {
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW = 720, physH = 1280, shortTarget = 720,
            maxW = 4096, maxH = 4096, wAlign = 2, hAlign = 2,
            maxPixels = 2_000_000,
        )
        assertEquals(720, w)
        assertEquals(1280, h)
    }

    // ── alignDown ────────────────────────────────────────────────────────────

    @Test
    fun `alignDown returns value unchanged when already aligned`() {
        assertEquals(1080, ScreenMirrorCaptureSize.alignDown(1080, 2))
        assertEquals(1088, ScreenMirrorCaptureSize.alignDown(1088, 16))
    }

    @Test
    fun `alignDown rounds down to nearest multiple`() {
        assertEquals(1078, ScreenMirrorCaptureSize.alignDown(1079, 2))
        assertEquals(1072, ScreenMirrorCaptureSize.alignDown(1079, 16))
        assertEquals(2304, ScreenMirrorCaptureSize.alignDown(2304, 2))
    }

    @Test
    fun `alignDown with align 1 is identity`() {
        assertEquals(42, ScreenMirrorCaptureSize.alignDown(42, 1))
    }
}
