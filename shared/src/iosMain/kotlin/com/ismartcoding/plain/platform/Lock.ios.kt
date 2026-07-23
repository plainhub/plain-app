package com.ismartcoding.plain.platform

import platform.Foundation.NSLock

actual fun newPlatformLock(): Any = NSLock()

actual fun <T> runPlatformLocked(lock: Any, block: () -> T): T {
    val nsLock = lock as NSLock
    nsLock.lock()
    try {
        return block()
    } finally {
        nsLock.unlock()
    }
}
