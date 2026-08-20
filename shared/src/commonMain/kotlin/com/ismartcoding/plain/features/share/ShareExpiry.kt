package com.ismartcoding.plain.features.share

import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Expiry options offered when creating a share link.
 */
enum class ShareExpiry(val hours: Long) {
    NEVER(0),
    HOUR_1(1),
    DAY_1(24),
    DAY_7(24 * 7),
    DAY_30(24 * 30);

    /** Absolute expiry instant relative to [now]; null = never expires. */
    fun expiresAt(now: Instant): Instant? =
        if (this == NEVER) null else now + hours.hours
}
