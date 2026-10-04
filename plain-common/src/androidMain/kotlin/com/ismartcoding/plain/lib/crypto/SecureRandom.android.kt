package com.ismartcoding.plain.lib.crypto

import java.security.SecureRandom

private val secureRandom = SecureRandom()

actual fun fillSecureRandom(buffer: ByteArray): Boolean {
    secureRandom.nextBytes(buffer)
    return true
}
