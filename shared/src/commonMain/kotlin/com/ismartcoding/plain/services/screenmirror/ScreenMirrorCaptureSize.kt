package com.ismartcoding.plain.services.screenmirror

/**
 * Pure capture-size helpers — no Android types so they're JVM-unit-testable
 * (mirrors the [H264AnnexB] pattern).
 *
 * Born from a Xiaomi Mi 9 (Android 11, Qualcomm OMX encoder) compatibility
 * bug: the screen is 1080×2340 but the OMX encoder's
 * `VideoCapabilities.supportedHeights` upper bound is 2304, so
 * `MediaCodec.configure()` rejected 1080×2340 with `EINVAL (-38)`.
 * Pixel 9 (Codec2 encoder) was unaffected.
 *
 * Added `maxPixels` constraint for older devices where the encoder is
 * technically capable of high resolutions but cannot sustain real-time
 * frame rates (e.g. Galaxy S9 encoding 1080×2220 at 60 fps).
 */
object ScreenMirrorCaptureSize {

    /**
     * Compute capture dimensions that fit within the physical screen, the
     * desired short-side target, the encoder's supported maximum, AND the
     * pixel budget.
     *
     * The scale factor is the minimum of five constraints so the result never
     * exceeds any single limit:
     * - `1.0` (never upscale beyond physical size)
     * - `shortTarget / min(physW, physH)` (honor quality preset)
     * - `maxW / physW` (encoder width limit)
     * - `maxH / physH` (encoder height limit)
     * - `sqrt(maxPixels / (physW * physH))` (pixel budget for real-time perf)
     *
     * Dimensions are then aligned down to the encoder's required alignment
     * (from `VideoCapabilities.widthAlignment` / `heightAlignment`).
     *
     * @param physW Physical screen width (from `getRealScreenSize`)
     * @param physH Physical screen height
     * @param shortTarget Target short-side resolution (1080 for HD, 720 for Smooth)
     * @param maxW Encoder `VideoCapabilities.supportedWidths.upper`
     * @param maxH Encoder `VideoCapabilities.supportedHeights.upper`
     * @param wAlign Encoder `VideoCapabilities.widthAlignment`
     * @param hAlign Encoder `VideoCapabilities.heightAlignment`
     * @param maxPixels Maximum pixel count for real-time encoding; devices
     *        with weaker encoders should pass a lower value. Use
     *        [Int.MAX_VALUE] to disable the pixel-budget constraint.
     * @return `(width, height)` aligned to the encoder's requirements
     */
    fun compute(
        physW: Int, physH: Int, shortTarget: Int,
        maxW: Int, maxH: Int, wAlign: Int, hAlign: Int,
        maxPixels: Int = Int.MAX_VALUE,
    ): Pair<Int, Int> {
        val physPixels = physW.toLong() * physH.toLong()
        val pixelScale = if (maxPixels == Int.MAX_VALUE || physPixels <= maxPixels) 1f
        else kotlin.math.sqrt(maxPixels.toFloat() / physPixels.toFloat())
        val scale = minOf(
            1f,
            shortTarget.toFloat() / minOf(physW, physH).toFloat(),
            maxW.toFloat() / physW.toFloat(),
            maxH.toFloat() / physH.toFloat(),
            pixelScale,
        )
        val w = alignDown((physW * scale).toInt().coerceAtLeast(wAlign), wAlign)
        val h = alignDown((physH * scale).toInt().coerceAtLeast(hAlign), hAlign)
        return Pair(w, h)
    }

    /**
     * Align a dimension down to the nearest multiple of [align].
     * Replaces the old `makeEven()` (which was `alignDown(v, 2)`) so we can
     * honor the encoder's actual alignment requirement (some OMX encoders
     * require 16-alignment).
     */
    fun alignDown(v: Int, align: Int): Int = (v / align) * align
}
