package com.ismartcoding.plain.helpers

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.nio.ByteBuffer

/**
 * Deterministic recognition tests: frames are synthesized by rendering real
 * ZXing-encoded codes into luminance buffers, so every assertion is byte-level
 * reproducible without a camera or wall clock.
 */
class QrScanPipelineTest {

    private val reader = QrScanPipeline.createReader()

    private fun whiteFrame(width: Int, height: Int) = ByteArray(width * height) { 255.toByte() }

    private fun qrMatrix(text: String, size: Int): BitMatrix =
        MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)

    private fun barcodeMatrix(text: String, width: Int, height: Int): BitMatrix =
        MultiFormatWriter().encode(text, BarcodeFormat.CODE_128, width, height)

    private fun draw(
        frame: ByteArray,
        frameWidth: Int,
        matrix: BitMatrix,
        left: Int,
        top: Int,
        invert: Boolean = false,
    ) {
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val dark = matrix.get(x, y)
                val value = if (dark xor invert) 0 else 255
                frame[(top + y) * frameWidth + (left + x)] = value.toByte()
            }
        }
    }

    private fun assertCenterNear(code: DecodedCode, expectedX: Float, expectedY: Float, tolerance: Float = 16f) {
        assertTrue(
            kotlin.math.abs(code.centerX - expectedX) <= tolerance,
            "centerX ${code.centerX} not near $expectedX",
        )
        assertTrue(
            kotlin.math.abs(code.centerY - expectedY) <= tolerance,
            "centerY ${code.centerY} not near $expectedY",
        )
    }

    @Test
    fun singleQrDecodesAtItsPosition() {
        val width = 480
        val height = 320
        val frame = whiteFrame(width, height)
        draw(frame, width, qrMatrix("https://example.com/plain", 120), left = 40, top = 60)
        val codes = QrScanPipeline.decode(reader, frame, width, height)
        assertEquals(1, codes.size)
        assertEquals("https://example.com/plain", codes[0].text)
        assertCenterNear(codes[0], 100f, 120f)
    }

    @Test
    fun twoQrCodesAreBothDecodedViaMasking() {
        val width = 480
        val height = 320
        val frame = whiteFrame(width, height)
        draw(frame, width, qrMatrix("https://example.com/link", 100), left = 20, top = 30)
        draw(frame, width, qrMatrix("82009", 100), left = 330, top = 190)
        val codes = QrScanPipeline.decode(reader, frame, width, height)
        assertEquals(2, codes.size)
        val texts = codes.map { it.text }.sorted()
        assertEquals(listOf("82009", "https://example.com/link"), texts)
        codes.first { it.text == "https://example.com/link" }.let { assertCenterNear(it, 70f, 80f) }
        codes.first { it.text == "82009" }.let { assertCenterNear(it, 380f, 240f) }
    }

    @Test
    fun qrAndBarcodeInOneFrameAreBothDecoded() {
        val width = 520
        val height = 360
        val frame = whiteFrame(width, height)
        draw(frame, width, qrMatrix("https://example.com/plain", 120), left = 40, top = 40)
        draw(frame, width, barcodeMatrix("82009", 260, 60), left = 220, top = 250)
        val codes = QrScanPipeline.decode(reader, frame, width, height)
        val texts = codes.map { it.text }.sorted()
        assertEquals(listOf("82009", "https://example.com/plain"), texts)
    }

    @Test
    fun invertedQrIsDecodedByFallbackPass() {
        val width = 320
        val height = 320
        val frame = ByteArray(width * height) { 0 } // black background
        draw(frame, width, qrMatrix("https://inv.example.com/82009", 160), left = 80, top = 80, invert = true)
        val codes = QrScanPipeline.decode(reader, frame, width, height)
        assertEquals(1, codes.size)
        assertEquals("https://inv.example.com/82009", codes[0].text)
    }

    @Test
    fun pathologicalNativeScaleIsRescuedByHalfScalePass() {
        // this content defeats ZXing's detector at a 160px render but decodes at 80px:
        // the half-scale retry pass must surface it with positions mapped back 2x
        val width = 320
        val height = 320
        val frame = whiteFrame(width, height)
        draw(frame, width, qrMatrix("https://inverted.example.com", 160), left = 80, top = 80)
        val codes = QrScanPipeline.decode(reader, frame, width, height)
        assertEquals(1, codes.size)
        assertEquals("https://inverted.example.com", codes[0].text)
        assertCenterNear(codes[0], 160f, 160f, tolerance = 20f)
    }

    @Test
    fun emptyFrameDecodesNothing() {
        val frame = whiteFrame(320, 240)
        assertTrue(QrScanPipeline.decode(reader, frame, 320, 240).isEmpty())
    }

    @Test
    fun rotationRoundTripsPixelExact() {
        val width = 12
        val height = 8
        val src = YPlane(ByteArray(width * height) { (it % 251).toByte() }, width, height)
        for (degrees in listOf(90, 180, 270)) {
            val rotated = QrScanPipeline.rotate(src, degrees, YPlane())
            val back = QrScanPipeline.rotate(rotated, 360 - degrees, YPlane())
            assertEquals(width, back.width)
            assertEquals(height, back.height)
            assertTrue(
                back.data.copyOf(width * height).contentEquals(src.data.copyOf(width * height)),
                "round trip through $degrees lost pixels",
            )
        }
    }

    @Test
    fun sensorRotationIsReversedAndCodeStillDecodesAtUprightPosition() {
        val width = 320
        val height = 240
        val upright = whiteFrame(width, height)
        draw(upright, width, qrMatrix("https://rotation.example.com", 80), left = 200, top = 60)
        // sensor frames are the upright image rotated 90° CW; undo by rotating back
        val sensor = QrScanPipeline.rotate(YPlane(upright, width, height), 90, YPlane())
        assertEquals(240, sensor.width)
        assertEquals(320, sensor.height)
        val frame = QrScanPipeline.rotate(sensor, 270, YPlane())
        assertEquals(width, frame.width)
        assertEquals(height, frame.height)
        val codes = QrScanPipeline.decode(reader, frame.data.copyOf(width * height), width, height)
        assertEquals(1, codes.size)
        assertEquals("https://rotation.example.com", codes[0].text)
        assertCenterNear(codes[0], 240f, 100f)
    }

    @Test
    fun rotationSwapsDimensions() {
        val src = YPlane(ByteArray(6 * 4), 6, 4)
        val r90 = QrScanPipeline.rotate(src, 90, YPlane())
        assertEquals(4, r90.width)
        assertEquals(6, r90.height)
        val r270 = QrScanPipeline.rotate(src, 270, YPlane())
        assertEquals(4, r270.width)
        assertEquals(6, r270.height)
        val r180 = QrScanPipeline.rotate(src, 180, YPlane())
        assertEquals(6, r180.width)
        assertEquals(4, r180.height)
    }

    @Test
    fun copyPackedYPlaneStripsRowPadding() {
        val rowStride = 12
        val width = 10
        val height = 4
        val raw = ByteArray(rowStride * height) { (it % 97).toByte() }
        val out = YPlane()
        QrScanPipeline.copyPackedYPlane(ByteBuffer.wrap(raw), width, height, rowStride, out)
        assertEquals(width, out.width)
        assertEquals(height, out.height)
        val packed = out.data.copyOf(width * height)
        for (row in 0 until height) {
            val expected = raw.copyOfRange(row * rowStride, row * rowStride + width)
            assertTrue(
                packed.copyOfRange(row * width, row * width + width).contentEquals(expected),
                "row $row not stripped correctly",
            )
        }
    }

    @Test
    fun coordMapperCoversAndClamps() {
        // same aspect: frame fills the view exactly
        run {
            val (x, y) = ScanCoordMapper.toViewNormalized(540f, 960f, 1080, 1920, 1080, 1920)
            assertEquals(0.5f, x)
            assertEquals(0.5f, y)
        }
        // view taller than 9:16 frame: horizontal crop of the frame
        run {
            val (x, y) = ScanCoordMapper.toViewNormalized(540f, 960f, 1080, 1920, 1080, 2400)
            assertEquals(0.5f, x)
            assertEquals(0.5f, y)
            val (cx, cy) = ScanCoordMapper.toViewNormalized(0f, 0f, 1080, 1920, 1080, 2400)
            assertEquals(0f, cx)
            assertEquals(0f, cy)
        }
        // landscape frame on a portrait view: horizontal overflow clamps to the edge
        run {
            val (x, y) = ScanCoordMapper.toViewNormalized(960f, 540f, 1920, 1080, 720, 1280)
            assertEquals(0.5f, x)
            assertEquals(0.5f, y)
            val (ex, _) = ScanCoordMapper.toViewNormalized(1920f, 0f, 1920, 1080, 720, 1280)
            assertEquals(1f, ex)
        }
        // degenerate sizes fall back to the center
        run {
            val (x, y) = ScanCoordMapper.toViewNormalized(5f, 5f, 0, 0, 100, 100)
            assertEquals(0.5f, x)
            assertEquals(0.5f, y)
        }
    }

    @Test
    fun decodeRejectsMalformedDimensions() {
        assertTrue(QrScanPipeline.decode(reader, ByteArray(10), 0, 0).isEmpty())
        assertTrue(QrScanPipeline.decode(reader, ByteArray(10), 4, 4).isEmpty())
    }

    @Test
    fun buildNv21SemiPlanarWithShortSliceLimits() {
        // interleaved UV allocation with slices whose limits stop one byte short of
        // a full contiguous row read — the exact S20 layout that underflowed before
        val width = 8
        val height = 4
        val chromaW = width / 2
        val chromaH = height / 2
        val chromaTotal = 2 * chromaW * chromaH
        val backing = ByteArray(width * height + chromaTotal)
        for (i in 0 until width * height) backing[i] = (i % 251).toByte()
        for (i in 0 until chromaTotal) backing[width * height + i] = (100 + i % 97).toByte()
        // U slice starts at the chroma base, V slice one byte in; both stop at the
        // last element they own (one byte short of a 2*chromaW read on the last row)
        val uBuffer = java.nio.ByteBuffer.wrap(backing, width * height, chromaTotal - 1).slice()
        val vBuffer = java.nio.ByteBuffer.wrap(backing, width * height + 1, chromaTotal - 1).slice()
        val nv21 = QrScanPipeline.buildNv21(
            java.nio.ByteBuffer.wrap(backing), width,
            uBuffer, chromaW * 2, 2,
            vBuffer, chromaW * 2, 2,
            width, height,
        )
        assertEquals(width * height + chromaTotal, nv21.size)
        // luma intact
        for (i in 0 until width * height) assertEquals(backing[i], nv21[i])
        // chroma interleave reads V,U pairs element-wise through each plane's strides
        for (i in 0 until chromaTotal / 2) {
            assertEquals(backing[width * height + 2 * i + 1], nv21[width * height + 2 * i])
            assertEquals(backing[width * height + 2 * i], nv21[width * height + 2 * i + 1])
        }
    }

    @Test
    fun buildNv21PlanarLayoutInterleavesVU() {
        val width = 8
        val height = 4
        val chromaW = width / 2
        val chromaH = height / 2
        val y = ByteArray(width * height) { (it % 89).toByte() }
        val u = ByteArray(chromaW * chromaH) { (it % 61).toByte() }
        val v = ByteArray(chromaW * chromaH) { ((it + 17) % 61).toByte() }
        val nv21 = QrScanPipeline.buildNv21(
            java.nio.ByteBuffer.wrap(y), width,
            java.nio.ByteBuffer.wrap(u), chromaW, 1,
            java.nio.ByteBuffer.wrap(v), chromaW, 1,
            width, height,
        )
        var pos = width * height
        for (i in 0 until chromaW * chromaH) {
            assertEquals(v[i], nv21[pos++])
            assertEquals(u[i], nv21[pos++])
        }
    }

    @Test
    fun buildNv21StripsRowPadding() {
        val width = 6
        val height = 4
        val rowStride = width + 4
        val padded = ByteArray(rowStride * height)
        for (row in 0 until height) {
            for (col in 0 until width) {
                padded[row * rowStride + col] = (row * 10 + col).toByte()
            }
        }
        val nv21 = QrScanPipeline.buildNv21(
            java.nio.ByteBuffer.wrap(padded), rowStride,
            java.nio.ByteBuffer.wrap(ByteArray(6) { (10 + it).toByte() }), 3, 1,
            java.nio.ByteBuffer.wrap(ByteArray(6) { (20 + it).toByte() }), 3, 1,
            width, height,
        )
        for (row in 0 until height) {
            for (col in 0 until width) {
                assertEquals((row * 10 + col).toByte(), nv21[row * width + col])
            }
        }
    }
}
