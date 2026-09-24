package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.lib.extensions.formatDurationSec
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for DLNA cast seek time format conversion.
 *
 * DLNA Seek (REL_TIME) expects HH:MM:SS format. [CastPlayer.parseTimeToMs]
 * parses this back to seconds for progress tracking. The round-trip must be
 * lossless for the seek workflow to work correctly.
 */
class CastPlayerSeekTimeTest {

    @Test
    fun formatDuration_alwaysShowHour_zeroSeconds() {
        assertEquals("00:00:00", 0L.formatDurationSec(alwaysShowHour = true))
    }

    @Test
    fun formatDuration_alwaysShowHour_secondsOnly() {
        assertEquals("00:00:05", 5L.formatDurationSec(alwaysShowHour = true))
        assertEquals("00:00:59", 59L.formatDurationSec(alwaysShowHour = true))
    }

    @Test
    fun formatDuration_alwaysShowHour_minutesAndSeconds() {
        assertEquals("00:01:00", 60L.formatDurationSec(alwaysShowHour = true))
        assertEquals("00:01:05", 65L.formatDurationSec(alwaysShowHour = true))
    }

    @Test
    fun formatDuration_alwaysShowHour_hoursMinutesSeconds() {
        assertEquals("01:00:00", 3600L.formatDurationSec(alwaysShowHour = true))
        assertEquals("01:01:01", 3661L.formatDurationSec(alwaysShowHour = true))
        assertEquals("10:30:45", (10 * 3600 + 30 * 60 + 45).toLong().formatDurationSec(alwaysShowHour = true))
    }

    @Test
    fun parseTimeToMs_validHms() {
        assertEquals(0f, CastPlayer.parseTimeToMs("00:00:00"))
        assertEquals(5_000f, CastPlayer.parseTimeToMs("00:00:05"))
        assertEquals(65_000f, CastPlayer.parseTimeToMs("00:01:05"))
        assertEquals(3_661_000f, CastPlayer.parseTimeToMs("01:01:01"))
    }

    @Test
    fun parseTimeToMs_emptyOrInvalid() {
        assertEquals(0f, CastPlayer.parseTimeToMs(""))
        assertEquals(0f, CastPlayer.parseTimeToMs("NOT_IMPLEMENTED"))
    }

    @Test
    fun roundTrip_hmsToMsAndBack() {
        val testCases = listOf(0L, 5_000L, 59_000L, 60_000L, 65_000L, 3_600_000L, 3_661_000L, (10 * 3600 + 30 * 60 + 45) * 1000L)
        for (ms in testCases) {
            val hms = (ms / 1000).formatDurationSec(alwaysShowHour = true)
            val parsed = CastPlayer.parseTimeToMs(hms)
            assertEquals(ms.toFloat(), parsed, "Round-trip failed for ${ms}ms: $hms")
        }
    }
}
