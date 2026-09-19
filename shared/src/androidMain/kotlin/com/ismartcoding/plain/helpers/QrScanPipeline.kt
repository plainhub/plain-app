package com.ismartcoding.plain.helpers

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.EnumMap
import kotlin.math.max
import kotlin.math.min

/** A code decoded from one luminance frame; position in frame pixels from the top-left. */
class DecodedCode(val text: String, val centerX: Float, val centerY: Float)

/** A packed (rowStride == width) luminance plane with reusable storage. */
class YPlane(var data: ByteArray = ByteArray(0), var width: Int = 0, var height: Int = 0) {
    fun ensureCapacity(capacity: Int) {
        if (data.size < capacity) data = ByteArray(capacity)
    }
}

/**
 * Pure ZXing scan pipeline shared by the camera analyzer and image decoding:
 * multi-code masking, inverted pass and reader hygiene. No Android dependencies.
 */
object QrScanPipeline {
    const val MAX_CODES = 4
    private const val MASK_FILL: Byte = 127
    private const val MIN_MASK_MARGIN_PX = 24
    private const val HALF_SCALE_MIN_DIM = 320

    private val FORMATS = listOf(
        BarcodeFormat.QR_CODE,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.AZTEC,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR,
    )

    fun createReader(): MultiFormatReader {
        val reader = MultiFormatReader()
        val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
        hints[DecodeHintType.TRY_HARDER] = true
        hints[DecodeHintType.POSSIBLE_FORMATS] = FORMATS
        reader.setHints(hints)
        return reader
    }

    /**
     * Decodes up to [maxCodes] codes from a packed luminance buffer. The buffer is
     * mutated: found regions are masked out and it is inverted for a fallback pass,
     * so callers must hand over ownership for the call.
     *
     * Recognition passes, in order: native scale, half scale (ZXing's detector
     * deterministically fails some clean codes at specific module pixel scales),
     * then both again inverted (white-on-black codes). Positions are always
     * returned in native frame pixels.
     */
    fun decode(
        reader: MultiFormatReader,
        luminance: ByteArray,
        width: Int,
        height: Int,
        maxCodes: Int = MAX_CODES,
    ): List<DecodedCode> {
        if (width <= 0 || height <= 0 || luminance.size < width * height) return emptyList()
        val out = ArrayList<DecodedCode>(2)
        decodePass(reader, luminance, width, height, maxCodes, out)
        var half: ByteArray? = null
        var halfWidth = 0
        var halfHeight = 0
        if (out.isEmpty() && minOf(width, height) >= HALF_SCALE_MIN_DIM) {
            halfWidth = width / 2
            halfHeight = height / 2
            val halfBuffer = halve(luminance, width, height, halfWidth, halfHeight)
            half = halfBuffer
            decodePass(reader, halfBuffer, halfWidth, halfHeight, maxCodes, out, scaleBack = 2f)
        }
        if (out.isEmpty()) {
            invert(luminance)
            decodePass(reader, luminance, width, height, maxCodes, out)
            val halfBuffer = half
            if (out.isEmpty() && halfBuffer != null) {
                invert(halfBuffer)
                decodePass(reader, halfBuffer, halfWidth, halfHeight, maxCodes, out, scaleBack = 2f)
            }
        }
        return out
    }

    private fun decodePass(
        reader: MultiFormatReader,
        luminance: ByteArray,
        width: Int,
        height: Int,
        maxCodes: Int,
        out: MutableList<DecodedCode>,
        scaleBack: Float = 1f,
    ) {
        var attempts = 0
        // hard cap: a re-decoded duplicate must not loop forever if masking misses it
        val maxAttempts = maxCodes * 2
        while (out.size < maxCodes && attempts < maxAttempts) {
            attempts++
            val result = decodeOnce(reader, luminance, width, height) ?: return
            val points = result.resultPoints
            if (points == null || points.isEmpty()) {
                return
            }
            if (out.none { it.text == result.text }) {
                out.add(DecodedCode(result.text, centerX(points) * scaleBack, centerY(points) * scaleBack))
            }
            maskRegion(luminance, width, height, points)
        }
    }

    private fun decodeOnce(reader: MultiFormatReader, luminance: ByteArray, width: Int, height: Int): Result? {
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        reader.reset()
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        } catch (e: ReaderException) {
            null
        }
    }

    private fun centerX(points: Array<ResultPoint>): Float {
        var sum = 0f
        for (p in points) sum += p.x
        return sum / points.size
    }

    private fun centerY(points: Array<ResultPoint>): Float {
        var sum = 0f
        for (p in points) sum += p.y
        return sum / points.size
    }

    /**
     * Masks the code region derived from the finder points so the next decode pass
     * finds other codes. Result points sit on finder/edge features inside the code,
     * so the margin covers the modules and quiet zone around them.
     */
    private fun maskRegion(luminance: ByteArray, width: Int, height: Int, points: Array<ResultPoint>) {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        for (p in points) {
            val x = p.x.toInt()
            val y = p.y.toInt()
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }
        val marginX = max(MIN_MASK_MARGIN_PX, (maxX - minX) * 3 / 10)
        val marginY = max(MIN_MASK_MARGIN_PX, (maxY - minY) * 3 / 10)
        val left = max(0, minX - marginX)
        val right = min(width - 1, maxX + marginX)
        val top = max(0, minY - marginY)
        val bottom = min(height - 1, maxY + marginY)
        for (y in top..bottom) {
            val rowStart = y * width
            for (x in left..right) {
                luminance[rowStart + x] = MASK_FILL
            }
        }
    }

    fun invert(luminance: ByteArray) {
        for (i in luminance.indices) {
            luminance[i] = (255 - (luminance[i].toInt() and 0xff)).toByte()
        }
    }

    /** 2x2 box-average downscale of a packed plane; odd edges clamp to the last pixel. */
    private fun halve(src: ByteArray, width: Int, height: Int, halfWidth: Int, halfHeight: Int): ByteArray {
        val out = ByteArray(halfWidth * halfHeight)
        for (y in 0 until halfHeight) {
            val row0 = 2 * y
            val row1 = min(height - 1, row0 + 1)
            for (x in 0 until halfWidth) {
                val col0 = 2 * x
                val col1 = min(width - 1, col0 + 1)
                val a = src[row0 * width + col0].toInt() and 0xff
                val b = src[row0 * width + col1].toInt() and 0xff
                val c = src[row1 * width + col0].toInt() and 0xff
                val d = src[row1 * width + col1].toInt() and 0xff
                out[y * halfWidth + x] = ((a + b + c + d) / 4).toByte()
            }
        }
        return out
    }

    /**
     * Assembles an NV21 frame from the planes of a YUV_420_888 camera image. The chroma
     * interleave reads each plane element-wise through its own strides: a contiguous
     * row copy would overflow the plane's limit by one byte on the final row of
     * semi-planar devices (S20 BufferUnderflowException, 2026-09-20), while per-element
     * access never exceeds a plane's own data extent and yields correct U/V values for
     * both planar and interleaved layouts.
     */
    fun buildNv21(
        y: ByteBuffer,
        yRowStride: Int,
        u: ByteBuffer,
        uRowStride: Int,
        uPixelStride: Int,
        v: ByteBuffer,
        vRowStride: Int,
        vPixelStride: Int,
        width: Int,
        height: Int,
    ): ByteArray {
        val chromaW = width / 2
        val chromaH = height / 2
        val out = ByteArray(width * height + 2 * chromaW * chromaH)
        if (yRowStride == width) {
            y.position(0)
            y.get(out, 0, width * height)
        } else {
            for (row in 0 until height) {
                y.position(row * yRowStride)
                y.get(out, row * width, width)
            }
        }
        var pos = width * height
        for (row in 0 until chromaH) {
            val vRow = row * vRowStride
            val uRow = row * uRowStride
            for (col in 0 until chromaW) {
                out[pos++] = v.get(vRow + col * vPixelStride)
                out[pos++] = u.get(uRow + col * uPixelStride)
            }
        }
        return out
    }

    /** Copies the Y plane of a camera frame into [out] as a packed plane, padding stripped. */
    fun copyPackedYPlane(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, out: YPlane) {
        out.ensureCapacity(width * height)
        out.width = width
        out.height = height
        val data = out.data
        if (rowStride == width) {
            buffer.position(0)
            buffer.get(data, 0, width * height)
            return
        }
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            buffer.get(data, row * width, width)
        }
    }

    /**
     * Rotates a packed luminance plane clockwise by [degrees] (one of 0/90/180/270),
     * mirroring how ImageInfo.rotationDegrees turns a sensor frame upright.
     */
    fun rotate(src: YPlane, degrees: Int, out: YPlane): YPlane {
        val w = src.width
        val h = src.height
        val s = src.data
        when (degrees) {
            0 -> {
                if (out !== src) {
                    out.ensureCapacity(w * h)
                    System.arraycopy(s, 0, out.data, 0, w * h)
                    out.width = w
                    out.height = h
                }
                return out
            }

            90 -> {
                out.ensureCapacity(w * h)
                val d = out.data
                for (y in 0 until h) {
                    val srcRow = y * w
                    for (x in 0 until w) {
                        d[x * h + (h - 1 - y)] = s[srcRow + x]
                    }
                }
                out.width = h
                out.height = w
            }

            180 -> {
                out.ensureCapacity(w * h)
                val d = out.data
                val last = w * h - 1
                for (i in 0 until w * h) {
                    d[last - i] = s[i]
                }
                out.width = w
                out.height = h
            }

            270 -> {
                out.ensureCapacity(w * h)
                val d = out.data
                for (y in 0 until h) {
                    val srcRow = y * w
                    for (x in 0 until w) {
                        d[(w - 1 - x) * h + y] = s[srcRow + x]
                    }
                }
                out.width = h
                out.height = w
            }

            else -> throw IllegalArgumentException("Unsupported rotation: $degrees")
        }
        return out
    }
}

/**
 * Maps a point in an (upright) analysis frame to normalized coordinates of the view
 * that renders the frame with aspect-fill (center-crop) scaling, as the viewfinder does.
 */
object ScanCoordMapper {
    fun toViewNormalized(
        x: Float,
        y: Float,
        frameWidth: Int,
        frameHeight: Int,
        viewWidth: Int,
        viewHeight: Int,
    ): Pair<Float, Float> {
        if (frameWidth <= 0 || frameHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return Pair(0.5f, 0.5f)
        }
        val scale = max(viewWidth.toFloat() / frameWidth, viewHeight.toFloat() / frameHeight)
        val offsetX = (viewWidth - frameWidth * scale) / 2f
        val offsetY = (viewHeight - frameHeight * scale) / 2f
        val vx = ((x * scale + offsetX) / viewWidth).coerceIn(0f, 1f)
        val vy = ((y * scale + offsetY) / viewHeight).coerceIn(0f, 1f)
        return Pair(vx, vy)
    }
}
