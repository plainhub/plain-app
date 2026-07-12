package com.ismartcoding.plain.platform

expect class PlatformLock() {
    fun <T> withLock(block: () -> T): T
}
