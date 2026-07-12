package com.ismartcoding.plain.ai

import kotlin.math.sqrt

internal fun l2Normalize(vec: FloatArray): FloatArray {
    var norm = 0f
    for (v in vec) norm += v * v
    norm = sqrt(norm)
    if (norm > 0f) for (i in vec.indices) vec[i] /= norm
    return vec
}

internal fun dotProduct(a: FloatArray, b: FloatArray): Float {
    if (a.size != b.size) return 0f
    var dot = 0f
    for (i in a.indices) dot += a[i] * b[i]
    return dot
}

internal fun hasInvalidValues(vec: FloatArray): Boolean {
    for (v in vec) if (v.isInfinite() || v.isNaN()) return true
    return false
}

internal fun floatsToBytes(floats: FloatArray): ByteArray {
    val buf = java.nio.ByteBuffer.allocate(floats.size * 4)
    for (f in floats) buf.putFloat(f)
    return buf.array()
}

internal fun bytesToFloats(bytes: ByteArray): FloatArray {
    val buf = java.nio.ByteBuffer.wrap(bytes)
    return FloatArray(bytes.size / 4) { buf.getFloat() }
}
