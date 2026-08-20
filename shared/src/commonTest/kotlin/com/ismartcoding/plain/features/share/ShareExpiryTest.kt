package com.ismartcoding.plain.features.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ShareExpiryTest {
    private val now: Instant = Instant.fromEpochMilliseconds(1_784_160_000_000L) // 2026-08-21T00:00:00Z

    @Test
    fun never_expires_returns_null() {
        assertNull(ShareExpiry.NEVER.expiresAt(now))
    }

    @Test
    fun hour_expiry_is_one_hour_after_now() {
        assertEquals(now + 1.hours, ShareExpiry.HOUR_1.expiresAt(now))
    }

    @Test
    fun day_expiry_is_24_hours_after_now() {
        assertEquals(now + 24.hours, ShareExpiry.DAY_1.expiresAt(now))
    }

    @Test
    fun week_expiry_is_168_hours_after_now() {
        assertEquals(now + 168.hours, ShareExpiry.DAY_7.expiresAt(now))
    }

    @Test
    fun month_expiry_is_720_hours_after_now() {
        assertEquals(now + 720.hours, ShareExpiry.DAY_30.expiresAt(now))
    }
}
