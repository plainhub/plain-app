package com.ismartcoding.plain.lib.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

// SecRandomCopyBytes returns errSecSuccess (0) on success.
@OptIn(ExperimentalForeignApi::class)
actual fun fillSecureRandom(buffer: ByteArray): Boolean {
    if (buffer.isEmpty()) return true
    val status =
        buffer.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, buffer.size.toULong(), pinned.addressOf(0))
        }
    return status == 0
}
