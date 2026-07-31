package com.ismartcoding.plain.crypto

import kotlin.experimental.xor

private val SIGMA = "expand 32-byte k".encodeToByteArray()

private fun rotl32(v: Int, c: Int): Int = (v shl c) or (v ushr (32 - c))

private fun quarterRound(s: IntArray, a: Int, b: Int, c: Int, d: Int) {
    s[a] += s[b]; s[d] = rotl32(s[d] xor s[a], 16)
    s[c] += s[d]; s[b] = rotl32(s[b] xor s[c], 12)
    s[a] += s[b]; s[d] = rotl32(s[d] xor s[a], 8)
    s[c] += s[d]; s[b] = rotl32(s[b] xor s[c], 7)
}

private fun u32Le(b: ByteArray, off: Int): Int =
    (b[off].toInt() and 0xff) or
        ((b[off + 1].toInt() and 0xff) shl 8) or
        ((b[off + 2].toInt() and 0xff) shl 16) or
        ((b[off + 3].toInt() and 0xff) shl 24)

private fun writeU32Le(b: ByteArray, off: Int, v: Int) {
    b[off] = v.toByte()
    b[off + 1] = (v ushr 8).toByte()
    b[off + 2] = (v ushr 16).toByte()
    b[off + 3] = (v ushr 24).toByte()
}

private fun chacha20Block(key: ByteArray, counter: Int, nonce12: ByteArray, out: ByteArray) {
    val s = IntArray(16)
    for (i in 0 until 4) s[i] = u32Le(SIGMA, i * 4)
    for (i in 0 until 8) s[4 + i] = u32Le(key, i * 4)
    s[12] = counter
    for (i in 0 until 3) s[13 + i] = u32Le(nonce12, i * 4)

    val x = s.copyOf()
    repeat(10) {
        quarterRound(x, 0, 4, 8, 12)
        quarterRound(x, 1, 5, 9, 13)
        quarterRound(x, 2, 6, 10, 14)
        quarterRound(x, 3, 7, 11, 15)
        quarterRound(x, 0, 5, 10, 15)
        quarterRound(x, 1, 6, 11, 12)
        quarterRound(x, 2, 7, 8, 13)
        quarterRound(x, 3, 4, 9, 14)
    }
    for (i in 0 until 16) writeU32Le(out, i * 4, x[i] + s[i])
}

private fun hChaCha20(key: ByteArray, nonce16: ByteArray): ByteArray {
    val s = IntArray(16)
    for (i in 0 until 4) s[i] = u32Le(SIGMA, i * 4)
    for (i in 0 until 8) s[4 + i] = u32Le(key, i * 4)
    for (i in 0 until 4) s[12 + i] = u32Le(nonce16, i * 4)

    val x = s.copyOf()
    repeat(10) {
        quarterRound(x, 0, 4, 8, 12)
        quarterRound(x, 1, 5, 9, 13)
        quarterRound(x, 2, 6, 10, 14)
        quarterRound(x, 3, 7, 11, 15)
        quarterRound(x, 0, 5, 10, 15)
        quarterRound(x, 1, 6, 11, 12)
        quarterRound(x, 2, 7, 8, 13)
        quarterRound(x, 3, 4, 9, 14)
    }

    val subKey = ByteArray(32)
    for (i in 0 until 4) writeU32Le(subKey, i * 4, x[i])
    for (i in 0 until 4) writeU32Le(subKey, 16 + i * 4, x[12 + i])
    return subKey
}

private fun chacha20Encrypt(key: ByteArray, nonce12: ByteArray, counter: Int, data: ByteArray): ByteArray {
    val out = ByteArray(data.size)
    var pos = 0
    var ctr = counter
    val block = ByteArray(64)
    while (pos < data.size) {
        chacha20Block(key, ctr, nonce12, block)
        val len = minOf(64, data.size - pos)
        for (i in 0 until len) out[pos + i] = data[pos + i] xor block[i]
        pos += len
        ctr++
    }
    return out
}

// ── Poly1305 ──────────────────────────────────────────────────────────

private fun poly1305Mac(key: ByteArray, data: ByteArray): ByteArray {
    val r = LongArray(5)
    val s = LongArray(5)

    // Clamp r
    r[0] = (u32Le(key, 0).toLong() and 0x3ffffff)
    r[1] = ((u32Le(key, 3).toLong() ushr 2) and 0x3ffff03)
    r[2] = ((u32Le(key, 6).toLong() ushr 4) and 0x3ffc0ff)
    r[3] = ((u32Le(key, 9).toLong() ushr 6) and 0x3f03fff)
    r[4] = ((u32Le(key, 12).toLong() ushr 8) and 0x00fffff)

    s[0] = u32Le(key, 16).toLong() and 0x3ffffff
    s[1] = ((u32Le(key, 19).toLong() ushr 2) and 0x3ffffff)
    s[2] = ((u32Le(key, 22).toLong() ushr 4) and 0x3ffffff)
    s[3] = ((u32Le(key, 25).toLong() ushr 6) and 0x3ffffff)
    s[4] = (u32Le(key, 28).toLong() ushr 8)

    val h = LongArray(5)
    val p = 1305 - 1 // 2^130 - 5 as 2^26 - 5 in each limb base

    var i = 0
    while (i < data.size) {
        val len = minOf(16, data.size - i)
        val block = ByteArray(17)
        for (j in 0 until len) block[j] = data[i + j]
        block[len] = 1

        // Add block to h (5 × 26-bit limbs from 17 bytes)
        h[0] += (block[0].toLong() and 0xff) or ((block[1].toLong() and 0xff) shl 8) or ((block[2].toLong() and 0xff) shl 16) or ((block[3].toLong() and 0x03) shl 24)
        h[1] += ((block[3].toLong() ushr 2) and 0x3f) or ((block[4].toLong() and 0xff) shl 6) or ((block[5].toLong() and 0xff) shl 14) or ((block[6].toLong() and 0x0f) shl 22)
        h[2] += ((block[6].toLong() ushr 4) and 0x0f) or ((block[7].toLong() and 0xff) shl 4) or ((block[8].toLong() and 0xff) shl 12) or ((block[9].toLong() and 0x3f) shl 20)
        h[3] += ((block[9].toLong() ushr 6) and 0x03) or ((block[10].toLong() and 0xff) shl 2) or ((block[11].toLong() and 0xff) shl 10) or ((block[12].toLong() and 0xff) shl 18)
        h[4] += (block[13].toLong() and 0xff) or ((block[14].toLong() and 0xff) shl 8) or ((block[15].toLong() and 0xff) shl 16) or ((block[16].toLong() and 0xff) shl 24)

        // h = h * r mod (2^130 - 5)
        val d0 = h[0] * r[0] + h[1] * 5 * r[4] + h[2] * 5 * r[3] + h[3] * 5 * r[2] + h[4] * 5 * r[1]
        val d1 = h[0] * r[1] + h[1] * r[0] + h[2] * 5 * r[4] + h[3] * 5 * r[3] + h[4] * 5 * r[2]
        val d2 = h[0] * r[2] + h[1] * r[1] + h[2] * r[0] + h[3] * 5 * r[4] + h[4] * 5 * r[3]
        val d3 = h[0] * r[3] + h[1] * r[2] + h[2] * r[1] + h[3] * r[0] + h[4] * 5 * r[4]
        val d4 = h[0] * r[4] + h[1] * r[3] + h[2] * r[2] + h[3] * r[1] + h[4] * r[0]

        // Carry + reduce
        var c = 0L
        h[0] = d0 and 0x3ffffff; c = d0 ushr 26
        h[1] = (d1 + c) and 0x3ffffff; c = (d1 + c) ushr 26
        h[2] = (d2 + c) and 0x3ffffff; c = (d2 + c) ushr 26
        h[3] = (d3 + c) and 0x3ffffff; c = (d3 + c) ushr 26
        h[4] = (d4 + c) and 0x3ffffff; c = (d4 + c) ushr 26
        h[0] += c * 5
        c = h[0] ushr 26; h[0] = h[0] and 0x3ffffff
        h[1] += c

        i += len
    }

    // Final reduction
    var c = h[1] ushr 26; h[1] = h[1] and 0x3ffffff; h[2] += c
    c = h[2] ushr 26; h[2] = h[2] and 0x3ffffff; h[3] += c
    c = h[3] ushr 26; h[3] = h[3] and 0x3ffffff; h[4] += c
    c = h[4] ushr 26; h[4] = h[4] and 0x3ffffff; h[0] += c * 5
    c = h[0] ushr 26; h[0] = h[0] and 0x3ffffff; h[1] += c

    // Compute h - p: if h >= p, subtract p
    var g = LongArray(5)
    g[0] = h[0] + 5; c = g[0] ushr 26; g[0] = g[0] and 0x3ffffff
    g[1] = h[1] + c; c = g[1] ushr 26; g[1] = g[1] and 0x3ffffff
    g[2] = h[2] + c; c = g[2] ushr 26; g[2] = g[2] and 0x3ffffff
    g[3] = h[3] + c; c = g[3] ushr 26; g[3] = g[3] and 0x3ffffff
    g[4] = h[4] + c - (1L shl 26)

    val mask = if (g[4] ushr 63 == 0L) -1L else 0L
    for (j in 0 until 5) {
        h[j] = (h[j] and mask.inv()) or (g[j] and mask)
    }

    // Serialize h + s
    val f = h[0] + s[0]; h[0] = f and 0x3ffffff
    f.let { h[1] += it ushr 26 }
    val f2 = h[1] + s[1]; h[1] = f2 and 0x3ffffff
    f2.let { h[2] += it ushr 26 }
    val f3 = h[2] + s[2]; h[2] = f3 and 0x3ffffff
    f3.let { h[3] += it ushr 26 }
    val f4 = h[3] + s[3]; h[3] = f4 and 0x3ffffff
    f4.let { h[4] += it ushr 26 }
    val f5 = h[4] + s[4]; h[4] = f5 and 0x3ffffff

    val tag = ByteArray(16)
    writeU32Le(tag, 0, (h[0].toInt() or (h[1].toInt() shl 26)))
    writeU32Le(tag, 4, ((h[1].toInt() ushr 6) or (h[2].toInt() shl 20)))
    writeU32Le(tag, 8, ((h[2].toInt() ushr 12) or (h[3].toInt() shl 14)))
    writeU32Le(tag, 12, ((h[3].toInt() ushr 18) or (h[4].toInt() shl 8)))
    return tag
}

// ── XChaCha20-Poly1305 AEAD ────────────────────────────────────────────

private fun poly1305PadLen(len: Int): Int = if (len % 16 == 0) 0 else 16 - (len % 16)

private fun poly1305KeyGen(key: ByteArray, nonce12: ByteArray): ByteArray {
    val block = ByteArray(64)
    chacha20Block(key, 0, nonce12, block)
    return block.copyOfRange(0, 32)
}

internal fun xChaCha20Poly1305Encrypt(key: ByteArray, nonce24: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray {
    val subKey = hChaCha20(key, nonce24.copyOfRange(0, 16))
    val nonce12 = ByteArray(12)
    nonce24.copyOfRange(16, 24).copyInto(nonce12, 4)

    val polyKey = poly1305KeyGen(subKey, nonce12)
    val ciphertext = chacha20Encrypt(subKey, nonce12, 1, plaintext)

    // Build Poly1305 input: aad || pad || ct || pad || len_aad || len_ct
    val macInput = ByteArray(
        aad.size + poly1305PadLen(aad.size) +
            ciphertext.size + poly1305PadLen(ciphertext.size) + 16
    )
    var off = 0
    aad.copyInto(macInput, off); off += aad.size + poly1305PadLen(aad.size)
    ciphertext.copyInto(macInput, off); off += ciphertext.size + poly1305PadLen(ciphertext.size)
    writeU32Le(macInput, off, aad.size); writeU32Le(macInput, off + 4, 0)
    writeU32Le(macInput, off + 8, ciphertext.size); writeU32Le(macInput, off + 12, 0)

    val tag = poly1305Mac(polyKey, macInput)

    return nonce24 + ciphertext + tag
}

internal fun xChaCha20Poly1305Decrypt(key: ByteArray, data: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray? {
    if (data.size < 24 + 16) return null
    val nonce24 = data.copyOfRange(0, 24)
    val ciphertext = data.copyOfRange(24, data.size - 16)
    val tag = data.copyOfRange(data.size - 16, data.size)

    val subKey = hChaCha20(key, nonce24.copyOfRange(0, 16))
    val nonce12 = ByteArray(12)
    nonce24.copyOfRange(16, 24).copyInto(nonce12, 4)

    val polyKey = poly1305KeyGen(subKey, nonce12)

    val macInput = ByteArray(
        aad.size + poly1305PadLen(aad.size) +
            ciphertext.size + poly1305PadLen(ciphertext.size) + 16
    )
    var off = 0
    aad.copyInto(macInput, off); off += aad.size + poly1305PadLen(aad.size)
    ciphertext.copyInto(macInput, off); off += ciphertext.size + poly1305PadLen(ciphertext.size)
    writeU32Le(macInput, off, aad.size); writeU32Le(macInput, off + 4, 0)
    writeU32Le(macInput, off + 8, ciphertext.size); writeU32Le(macInput, off + 12, 0)

    val expectedTag = poly1305Mac(polyKey, macInput)

    // Constant-time tag comparison
    var diff = 0
    for (i in 0 until 16) diff = diff or (tag[i].toInt() xor expectedTag[i].toInt())
    if (diff != 0) return null

    return chacha20Encrypt(subKey, nonce12, 1, ciphertext)
}

// ── Internal test helpers ─────────────────────────────────────────────

internal fun chacha20EncryptInternal(key: ByteArray, nonce12: ByteArray, counter: Int, data: ByteArray): ByteArray =
    chacha20Encrypt(key, nonce12, counter, data)

internal fun poly1305MacInternal(key: ByteArray, data: ByteArray): ByteArray =
    poly1305Mac(key, data)

internal fun hChaCha20Internal(key: ByteArray, nonce16: ByteArray): ByteArray =
    hChaCha20(key, nonce16)

internal fun chaCha20Poly1305EncryptInternal(key: ByteArray, nonce12: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
    val polyKey = poly1305KeyGen(key, nonce12)
    val ciphertext = chacha20Encrypt(key, nonce12, 1, plaintext)
    val macInput = ByteArray(
        aad.size + poly1305PadLen(aad.size) +
            ciphertext.size + poly1305PadLen(ciphertext.size) + 16
    )
    var off = 0
    aad.copyInto(macInput, off); off += aad.size + poly1305PadLen(aad.size)
    ciphertext.copyInto(macInput, off); off += ciphertext.size + poly1305PadLen(ciphertext.size)
    writeU32Le(macInput, off, aad.size); writeU32Le(macInput, off + 4, 0)
    writeU32Le(macInput, off + 8, ciphertext.size); writeU32Le(macInput, off + 12, 0)
    val tag = poly1305Mac(polyKey, macInput)
    return ciphertext + tag
}
