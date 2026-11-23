package com.ismartcoding.plain.ui.media

import kotlin.io.encoding.Base64
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

internal data class ThumbHashPixels(val width: Int, val height: Int, val rgba: ByteArray)

internal fun decodeThumbHash(encoded: String): ThumbHashPixels? {
    if (encoded.length !in 8..36) return null
    val hash = try {
        Base64.decode(encoded)
    } catch (_: IllegalArgumentException) {
        return null
    }
    if (hash.size < 5) return null
    fun byte(index: Int) = hash[index].toInt() and 255

    val luminance = byte(0) or (byte(1) shl 8) or (byte(2) shl 16)
    val chroma = byte(3) or (byte(4) shl 8)
    val hasAlpha = luminance and (1 shl 23) != 0
    val landscape = chroma and (1 shl 15) != 0
    val maxTerms = if (hasAlpha) 5 else 7
    val shortTerms = chroma and 7
    if (shortTerms !in 1..maxTerms) return null
    val lx = if (landscape) maxTerms else shortTerms
    val ly = if (landscape) shortTerms else maxTerms
    val clx = lx.coerceAtLeast(3)
    val cly = ly.coerceAtLeast(3)

    val lTerms = coefficients(clx, cly)
    val colorTerms = coefficients(3, 3)
    val alphaTerms = if (hasAlpha) coefficients(5, 5) else emptyList()
    val offset = if (hasAlpha) 6 else 5
    val termCount = lTerms.size + 2 * colorTerms.size + alphaTerms.size
    if (hash.size != offset + (termCount + 1) / 2) return null

    var coefficientIndex = 0
    fun values(terms: List<Pair<Int, Int>>, scale: Double) = DoubleArray(terms.size) {
        val packed = byte(offset + coefficientIndex / 2)
        val nibble = (packed shr (4 * (coefficientIndex % 2))) and 15
        coefficientIndex++
        (nibble / 7.5 - 1.0) * scale
    }

    val lValues = values(lTerms, ((luminance shr 18) and 31) / 31.0)
    val pValues = values(colorTerms, ((chroma shr 3) and 63) / 63.0 * 1.25)
    val qValues = values(colorTerms, ((chroma shr 9) and 63) / 63.0 * 1.25)
    val alpha = if (hasAlpha) byte(5) else 15
    val aValues = values(alphaTerms, (alpha shr 4) / 15.0)
    val width = (32.0 * lx / maxOf(lx, ly)).roundToInt()
    val height = (32.0 * ly / maxOf(lx, ly)).roundToInt()
    val bx = basis(width)
    val by = basis(height)
    val rgba = ByteArray(width * height * 4)

    fun reconstruct(dc: Double, terms: List<Pair<Int, Int>>, ac: DoubleArray, x: Int, y: Int): Double {
        var result = dc
        for (index in terms.indices) {
            val (cx, cy) = terms[index]
            result += ac[index] * bx[x][cx] * by[y][cy] * 2
        }
        return result
    }

    fun channel(value: Double) = (value.coerceIn(0.0, 1.0) * 255).toInt().toByte()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val l = reconstruct((luminance and 63) / 63.0, lTerms, lValues, x, y)
            val p = reconstruct(((luminance shr 6) and 63) / 31.5 - 1, colorTerms, pValues, x, y)
            val q = reconstruct(((luminance shr 12) and 63) / 31.5 - 1, colorTerms, qValues, x, y)
            val a = reconstruct((alpha and 15) / 15.0, alphaTerms, aValues, x, y)
            val blue = l - (2.0 / 3.0) * p
            val red = (3 * l - blue + q) / 2
            val offset = 4 * (y * width + x)
            rgba[offset] = channel(red)
            rgba[offset + 1] = channel(red - q)
            rgba[offset + 2] = channel(blue)
            rgba[offset + 3] = channel(a)
        }
    }
    return ThumbHashPixels(width, height, rgba)
}

private fun coefficients(nx: Int, ny: Int): List<Pair<Int, Int>> = buildList {
    for (cy in 0 until ny) {
        val first = if (cy == 0) 1 else 0
        for (cx in first until nx) {
            if (cx * ny + cy * nx >= nx * ny) break
            add(cx to cy)
        }
    }
}

private fun basis(length: Int) = Array(length) { position ->
    DoubleArray(7) { frequency -> cos(PI / length * (position + 0.5) * frequency) }
}
