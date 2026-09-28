package com.ismartcoding.plain.lib.crypto

private const val HMAC_BLOCK_SIZE = 64

/**
 * HMAC-SHA256 over [data] keyed by [key], built on the platform-agnostic
 * [sha256] primitive so it behaves identically on Android/iOS.
 */
fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val block = if (key.size > HMAC_BLOCK_SIZE) sha256(key) else key
    val keyBlock = ByteArray(HMAC_BLOCK_SIZE).also { block.copyInto(it) }
    val iPadded = keyBlock.map { (it.toInt() xor 0x36).toByte() }.toByteArray()
    val oPadded = keyBlock.map { (it.toInt() xor 0x5c).toByte() }.toByteArray()
    return sha256(oPadded + sha256(iPadded + data))
}