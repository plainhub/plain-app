package com.ismartcoding.plain.crypto

/**
 * Pure-Kotlin SHA-1 implementation returning raw 20-byte digest.
 *
 * SHA-1 is cryptographically broken for collision resistance and should NOT
 * be used for security-critical purposes; it is retained only for legacy
 * non-security uses (e.g. deriving a stable file path from a URL) where the
 * digest is treated as an opaque key, not as a security guarantee.
 */
internal fun sha1(input: ByteArray): ByteArray {
    val h = intArrayOf(
        0x67452301, 0xEFCDAB89.toInt(), 0x98BADCFE.toInt(), 0x10325476, 0xC3D2E1F0.toInt(),
    )

    val bitLen = input.size.toLong() * 8
    val paddedLen = ((input.size + 8) / 64 + 1) * 64
    val padded = ByteArray(paddedLen)
    input.copyInto(padded)
    padded[input.size] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedLen - 1 - i] = (bitLen shr (i * 8)).toByte()
    }

    val w = IntArray(80)
    for (block in padded.indices step 64) {
        for (i in 0 until 16) {
            w[i] = ((padded[block + i * 4].toInt() and 0xFF) shl 24) or
                ((padded[block + i * 4 + 1].toInt() and 0xFF) shl 16) or
                ((padded[block + i * 4 + 2].toInt() and 0xFF) shl 8) or
                (padded[block + i * 4 + 3].toInt() and 0xFF)
        }
        for (i in 16 until 80) {
            w[i] = rotateLeft(w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16], 1)
        }

        var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]; var e = h[4]
        for (i in 0 until 80) {
            val (f, k) = when (i) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                in 20..39 -> (b xor c xor d) to 0x6ED9EBA1.toInt()
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) to 0xCA62C1D6.toInt()
            }
            val temp = rotateLeft(a, 5) + f + e + k + w[i]
            e = d; d = c; c = rotateLeft(b, 30); b = a; a = temp
        }
        h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e
    }

    val result = ByteArray(20)
    for (i in 0 until 5) {
        result[i * 4] = (h[i] ushr 24).toByte()
        result[i * 4 + 1] = (h[i] ushr 16).toByte()
        result[i * 4 + 2] = (h[i] ushr 8).toByte()
        result[i * 4 + 3] = h[i].toByte()
    }
    return result
}

private fun rotateLeft(x: Int, n: Int): Int = (x shl n) or (x ushr (32 - n))
