package com.ismartcoding.plain.platform

actual class PlatformLock {
    private val lock = Any()
    actual fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}
