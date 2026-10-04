package com.ismartcoding.plain.lib.crypto

/**
 * Fills [buffer] with cryptographically secure random bytes.
 * Returns false if the platform RNG call fails.
 */
expect fun fillSecureRandom(buffer: ByteArray): Boolean
