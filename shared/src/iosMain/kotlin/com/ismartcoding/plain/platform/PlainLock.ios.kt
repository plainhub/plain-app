package com.ismartcoding.plain.platform

/**
 * iOS actual: SMS/MMS state machines are Android-only, so no real exclusion is
 * needed here. Add a POSIX mutex if this is ever used by iOS code paths.
 */
actual class PlainLock actual constructor() {
    actual fun <T> withLock(block: () -> T): T = block()
}
