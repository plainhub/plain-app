package com.ismartcoding.plain.platform

class PlatformLock {
    private val delegate = newPlatformLock()

    fun <T> withLock(block: () -> T): T = runPlatformLocked(delegate, block)
}

expect fun newPlatformLock(): Any

expect fun <T> runPlatformLocked(lock: Any, block: () -> T): T
