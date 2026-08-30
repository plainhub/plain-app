package com.ismartcoding.plain.platform

actual class PlainLock actual constructor() {
    private val monitor = Any()

    actual fun <T> withLock(block: () -> T): T = synchronized(monitor) { block() }
}
