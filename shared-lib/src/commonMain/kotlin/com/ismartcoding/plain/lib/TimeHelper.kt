package com.ismartcoding.plain.lib

import kotlin.time.Clock
import kotlin.time.Instant

object TimeHelper {
    fun now(): Instant = Clock.System.now()
    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}