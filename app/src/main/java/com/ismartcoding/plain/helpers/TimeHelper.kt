package com.ismartcoding.plain.helpers

import kotlin.time.Instant

object TimeHelper {
    fun now(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
}
