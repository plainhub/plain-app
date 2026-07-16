package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the `Long.format*` extension functions used by file/audio
 * metadata display throughout the app (file sizes, bitrates, durations).
 *
 * These formatters are pure functions over `Long` — no platform dependencies,
 * so they are covered by host JVM tests.
 */
class LongFormatTest {

    // ---------- formatBytes ----------

    @Test
    fun formatBytes_zero() {
        assertEquals("0 B", 0L.formatBytes())
    }

    @Test
    fun formatBytes_smallValueNoPrefix() {
        assertEquals("1 B", 1L.formatBytes())
        assertEquals("999 B", 999L.formatBytes())
    }

    @Test
    fun formatBytes_kiloBoundary() {
        // 1000 B => 1.0 kB (SI units, not binary 1024)
        assertEquals("1.0 kB", 1000L.formatBytes())
    }

    @Test
    fun formatBytes_mega() {
        assertEquals("1.0 MB", 1_000_000L.formatBytes())
        assertEquals("1.5 MB", 1_500_000L.formatBytes())
    }

    @Test
    fun formatBytes_giga() {
        assertEquals("1.0 GB", 1_000_000_000L.formatBytes())
    }

    @Test
    fun formatBytes_decimalPrecision() {
        // Values < 999950 stay in current unit, so 999949 B shows as 999.9 kB
        assertEquals("999.9 kB", 999_949L.formatBytes())
    }

    // ---------- formatBitrate ----------

    @Test
    fun formatBitrate_smallValue() {
        assertEquals("0 bit/s", 0L.formatBitrate())
        assertEquals("999 bit/s", 999L.formatBitrate())
    }

    @Test
    fun formatBitrate_kilo() {
        assertEquals("1.0 kbit/s", 1000L.formatBitrate())
    }

    @Test
    fun formatBitrate_mega() {
        assertEquals("1.0 Mbit/s", 1_000_000L.formatBitrate())
    }

    @Test
    fun formatBitrate_giga() {
        assertEquals("1.0 Gbit/s", 1_000_000_000L.formatBitrate())
    }

    // ---------- formatDuration ----------

    @Test
    fun formatDuration_zeroSeconds() {
        assertEquals("00:00", 0L.formatDuration())
    }

    @Test
    fun formatDuration_secondsOnly() {
        assertEquals("00:05", 5L.formatDuration())
        assertEquals("00:59", 59L.formatDuration())
    }

    @Test
    fun formatDuration_minutesAndSeconds() {
        assertEquals("01:00", 60L.formatDuration())
        assertEquals("01:05", 65L.formatDuration())
        assertEquals("59:59", (59 * 60 + 59).toLong().formatDuration())
    }

    @Test
    fun formatDuration_hoursMinutesSeconds() {
        assertEquals("01:00:00", 3600L.formatDuration())
        assertEquals("01:01:01", 3661L.formatDuration())
    }

    @Test
    fun formatDuration_alwaysShowHour() {
        // With alwaysShowHour=true, even short durations include hour component.
        assertEquals("00:00:05", 5L.formatDuration(alwaysShowHour = true))
        assertEquals("00:05:00", 300L.formatDuration(alwaysShowHour = true))
    }

    @Test
    fun formatDuration_noHourWhenNotForced() {
        // Without alwaysShowHour, durations < 1 hour omit the hour field.
        assertEquals("05:00", 300L.formatDuration())
    }

    // ---------- formatMinSec ----------

    @Test
    fun formatMinSec_zero() {
        assertEquals("00:00", 0L.formatMinSec())
    }

    @Test
    fun formatMinSec_millisecondsTruncated() {
        // 59999 ms = 59 seconds after /1000 truncation
        assertEquals("00:59", 59_999L.formatMinSec())
    }

    @Test
    fun formatMinSec_oneMinute() {
        assertEquals("01:00", 60_000L.formatMinSec())
    }

    @Test
    fun formatMinSec_minutesAndSeconds() {
        assertEquals("01:30", 90_000L.formatMinSec())
        assertEquals("10:00", 600_000L.formatMinSec())
    }

    @Test
    fun formatMinSec_hoursAsMinutes() {
        // 1 hour = 60:00 in min:sec format (no hour field)
        assertEquals("60:00", 3_600_000L.formatMinSec())
    }

    @Test
    fun formatMinSec_padsToTwoDigits() {
        assertEquals("01:01", 61_000L.formatMinSec())
        assertEquals("01:05", 65_000L.formatMinSec())
    }
}
