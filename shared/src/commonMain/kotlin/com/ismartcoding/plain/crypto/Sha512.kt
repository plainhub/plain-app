package com.ismartcoding.plain.crypto

/**
 * Pure-Kotlin SHA-512 implementation returning raw 64-byte digest.
 *
 * Used by pairing crypto on platforms without a native SHA-512 entry point
 * (e.g. iOS CommonCrypto is not exposed through the default Kotlin/Native
 * cinterop bindings). Mirrors the algorithm of `sha256` but operates on
 * 64-bit words with a 1024-bit block size per FIPS 180-4.
 */
internal fun sha512(input: ByteArray): ByteArray {
    val h = longArrayOf(
        0x6a09e667f3bcc908uL.toLong(), 0xbb67ae8584caa73buL.toLong(),
        0x3c6ef372fe94f82buL.toLong(), 0xa54ff53a5f1d36f1uL.toLong(),
        0x510e527fade682d1uL.toLong(), 0x9b05688c2b3e6c1fuL.toLong(),
        0x1f83d9abfb41bd6buL.toLong(), 0x5be0cd19137e2179uL.toLong(),
    )

    val k = longArrayOf(
        0x428a2f98d728ae22uL.toLong(), 0x7137449123ef65cduL.toLong(),
        0xb5c0fbcfec4d3b2fuL.toLong(), 0xe9b5dba58189dbbcuL.toLong(),
        0x3956c25bf348b538uL.toLong(), 0x59f111f1b605d019uL.toLong(),
        0x923f82a4af194f9buL.toLong(), 0xab1c5ed5da6d8118uL.toLong(),
        0xd807aa98a3030242uL.toLong(), 0x12835b0145706fbeuL.toLong(),
        0x243185be4ee4b28cuL.toLong(), 0x550c7dc3d5ffb4e2uL.toLong(),
        0x72be5d74f27b896fuL.toLong(), 0x80deb1fe3b1696b1uL.toLong(),
        0x9bdc06a725c71235uL.toLong(), 0xc19bf174cf692694uL.toLong(),
        0xe49b69c19ef14ad2uL.toLong(), 0xefbe4786384f25e3uL.toLong(),
        0x0fc19dc68b8cd5b5uL.toLong(), 0x240ca1cc77ac9c65uL.toLong(),
        0x2de92c6f592b0275uL.toLong(), 0x4a7484aa6ea6e483uL.toLong(),
        0x5cb0a9dcbd41fbd4uL.toLong(), 0x76f988da831153b5uL.toLong(),
        0x983e5152ee66dfabuL.toLong(), 0xa831c66d2db43210uL.toLong(),
        0xb00327c898fb213fuL.toLong(), 0xbf597fc7beef0ee4uL.toLong(),
        0xc6e00bf33da88fc2uL.toLong(), 0xd5a79147930aa725uL.toLong(),
        0x06ca6351e003826fuL.toLong(), 0x142929670a0e6e70uL.toLong(),
        0x27b70a8546d22ffcuL.toLong(), 0x2e1b21385c26c926uL.toLong(),
        0x4d2c6dfc5ac42aeduL.toLong(), 0x53380d139d95b3dfuL.toLong(),
        0x650a73548baf63deuL.toLong(), 0x766a0abb3c77b2a8uL.toLong(),
        0x81c2c92e47edaee6uL.toLong(), 0x92722c851482353buL.toLong(),
        0xa2bfe8a14cf10364uL.toLong(), 0xa81a664bbc423001uL.toLong(),
        0xc24b8b70d0f89791uL.toLong(), 0xc76c51a30654be30uL.toLong(),
        0xd192e819d6ef5218uL.toLong(), 0xd69906245565a910uL.toLong(),
        0xf40e35855771202auL.toLong(), 0x106aa07032bbd1b8uL.toLong(),
        0x19a4c116b8d2d0c8uL.toLong(), 0x1e376c085141ab53uL.toLong(),
        0x2748774cdf8eeb99uL.toLong(), 0x34b0bcb5e19b48a8uL.toLong(),
        0x391c0cb3c5c95a63uL.toLong(), 0x4ed8aa4ae3418acbuL.toLong(),
        0x5b9cca4f7763e373uL.toLong(), 0x682e6ff3d6b2b8a3uL.toLong(),
        0x748f82ee5defb2fcuL.toLong(), 0x78a5636f43172f60uL.toLong(),
        0x84c87814a1f0ab72uL.toLong(), 0x8cc702081a6439ecuL.toLong(),
        0x90befffa23631e28uL.toLong(), 0xa4506cebde82bde9uL.toLong(),
        0xbef9a3f7b2c67915uL.toLong(), 0xc67178f2e372532buL.toLong(),
        0xca273eceea26619cuL.toLong(), 0xd186b8c721c0c207uL.toLong(),
        0xeada7dd6cde0eb1euL.toLong(), 0xf57d4f7fee6ed178uL.toLong(),
        0x06f067aa72176fbauL.toLong(), 0x0a637dc5a2c898a6uL.toLong(),
        0x113f9804bef90daeuL.toLong(), 0x1b710b35131c471buL.toLong(),
        0x28db77f523047d84uL.toLong(), 0x32caab7b40c72493uL.toLong(),
        0x3c9ebe0a15c9bebcuL.toLong(), 0x431d67c49c100d4cuL.toLong(),
        0x4cc5d4becb3e42b6uL.toLong(), 0x597f299cfc657e2auL.toLong(),
        0x5fcb6fab3ad6faecuL.toLong(), 0x6c44198c4a475817uL.toLong(),
    )

    // SHA-512 uses 128-byte blocks and a 16-byte length field.
    val bitLen = input.size.toLong() * 8
    val paddedLen = ((input.size + 16) / 128 + 1) * 128
    val padded = ByteArray(paddedLen)
    input.copyInto(padded)
    padded[input.size] = 0x80.toByte()
    // 16-byte big-endian length: high 8 bytes are zero (input bitLen fits in 63 bits)
    for (i in 0 until 8) {
        padded[paddedLen - 1 - i] = (bitLen shr (i * 8)).toByte()
    }

    val w = LongArray(80)
    for (block in padded.indices step 128) {
        for (i in 0 until 16) {
            val base = block + i * 8
            w[i] = ((padded[base].toLong() and 0xFF) shl 56) or
                ((padded[base + 1].toLong() and 0xFF) shl 48) or
                ((padded[base + 2].toLong() and 0xFF) shl 40) or
                ((padded[base + 3].toLong() and 0xFF) shl 32) or
                ((padded[base + 4].toLong() and 0xFF) shl 24) or
                ((padded[base + 5].toLong() and 0xFF) shl 16) or
                ((padded[base + 6].toLong() and 0xFF) shl 8) or
                (padded[base + 7].toLong() and 0xFF)
        }
        for (i in 16 until 80) {
            val s0 = rotateRight64(w[i - 15], 1) xor rotateRight64(w[i - 15], 8) xor (w[i - 15] ushr 7)
            val s1 = rotateRight64(w[i - 2], 19) xor rotateRight64(w[i - 2], 61) xor (w[i - 2] ushr 6)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }

        var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
        var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
        for (i in 0 until 80) {
            val s1 = rotateRight64(e, 14) xor rotateRight64(e, 18) xor rotateRight64(e, 41)
            val ch = (e and f) xor (e.inv() and g)
            val t1 = hh + s1 + ch + k[i] + w[i]
            val s0 = rotateRight64(a, 28) xor rotateRight64(a, 34) xor rotateRight64(a, 39)
            val maj = (a and b) xor (a and c) xor (b and c)
            val t2 = s0 + maj
            hh = g; g = f; f = e; e = d + t1
            d = c; c = b; b = a; a = t1 + t2
        }
        h[0] += a; h[1] += b; h[2] += c; h[3] += d
        h[4] += e; h[5] += f; h[6] += g; h[7] += hh
    }

    val result = ByteArray(64)
    for (i in 0 until 8) {
        result[i * 8] = (h[i] ushr 56).toByte()
        result[i * 8 + 1] = (h[i] ushr 48).toByte()
        result[i * 8 + 2] = (h[i] ushr 40).toByte()
        result[i * 8 + 3] = (h[i] ushr 32).toByte()
        result[i * 8 + 4] = (h[i] ushr 24).toByte()
        result[i * 8 + 5] = (h[i] ushr 16).toByte()
        result[i * 8 + 6] = (h[i] ushr 8).toByte()
        result[i * 8 + 7] = h[i].toByte()
    }
    return result
}

private fun rotateRight64(x: Long, n: Int): Long = (x ushr n) or (x shl (64 - n))
