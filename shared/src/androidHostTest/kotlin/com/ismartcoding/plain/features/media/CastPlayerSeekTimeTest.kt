package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.lib.extensions.formatDuration
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for DLNA cast seek time format conversion.
 *
 * DLNA Seek (REL_TIME) expects HH:MM:SS format. [CastPlayer.parseTimeToSeconds]
 * parses this back to seconds for progress tracking. The round-trip must be
 * lossless for the seek workflow to work correctly.
 */
class CastPlayerSeekTimeTest {

    @Test
    fun formatDuration_alwaysShowHour_zeroSeconds() {
        assertEquals("00:00:00", 0L.formatDuration(alwaysShowHour = true))
    }

    @Test
    fun formatDuration_alwaysShowHour_secondsOnly() {
        assertEquals("00:00:05", 5L.formatDuration(alwaysShowHour = true))
        assertEquals("00:00:59", 59L.formatDuration(alwaysShowHour = true))
    }

    @Test
    fun formatDuration_alwaysShowHour_minutesAndSeconds() {
        assertEquals("00:01:00", 60L.formatDuration(alwaysShowHour = true))
        assertEquals("00:01:05", 65L.formatDuration(alwaysShowHour = true))
    }

    @Test
    fun formatDuration_alwaysShowHour_hoursMinutesSeconds() {
        assertEquals("01:00:00", 3600L.formatDuration(alwaysShowHour = true))
        assertEquals("01:01:01", 3661L.formatDuration(alwaysShowHour = true))
        assertEquals("10:30:45", (10 * 3600 + 30 * 60 + 45).toLong().formatDuration(alwaysShowHour = true))
    }

    @Test
    fun parseTimeToSeconds_validHms() {
        assertEquals(0f, CastPlayer.parseTimeToSeconds("00:00:00"))
        assertEquals(5f, CastPlayer.parseTimeToSeconds("00:00:05"))
        assertEquals(65f, CastPlayer.parseTimeToSeconds("00:01:05"))
        assertEquals(3661f, CastPlayer.parseTimeToSeconds("01:01:01"))
    }

    @Test
    fun parseTimeToSeconds_emptyOrInvalid() {
        assertEquals(0f, CastPlayer.parseTimeToSeconds(""))
        assertEquals(0f, CastPlayer.parseTimeToSeconds("NOT_IMPLEMENTED"))
    }

    @Test
    fun roundTrip_secondsToHmsAndBack() {
        val testCases = listOf(0L, 5L, 59L, 60L, 65L, 3600L, 3661L, 10 * 3600 + 30 * 60 + 45L)
        for (seconds in testCases) {
            val hms = seconds.formatDuration(alwaysShowHour = true)
            val parsed = CastPlayer.parseTimeToSeconds(hms)
            assertEquals(seconds.toFloat(), parsed, "Round-trip failed for $seconds seconds: $hms")
        }
    }
}
