package com.ismartcoding.plain.platform

/**
 * Minimal multiplatform mutual-exclusion lock. On Android this maps to a JVM
 * monitor (`synchronized`); on iOS the lock is a no-op because the code paths
 * guarded by it (SMS/MMS state machines) never run there.
 */
expect class PlainLock() {
    fun <T> withLock(block: () -> T): T
}
