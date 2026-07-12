package com.ismartcoding.plain.platform

import platform.Foundation.NSLock

actual class PlatformLock {
    private val lock = NSLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
