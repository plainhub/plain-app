package com.ismartcoding.plain.helpers

import kotlinx.datetime.Instant

object TimeHelper {
    fun now(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
}
