package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * A share link. [id] doubles as the public `shared_id` that appears in the
 * link path. The `shared_token` is never stored – it is derived on demand via
 * HMAC(masterSecret, id), and [urlToken] is the dedicated key for guest `/fs` / `/zip/dir`.
 */
data class DShare(
    var id: String, // = shared_id

    var name: String = "",

    /** Reserved for a future password feature; unused this release. Stored in plaintext. */
    var password: String = "",

    var urlToken: String = "",

    /** Valid until this instant; null = never expires. */
    var expiresAt: Instant? = null,

    var readOnly: Boolean = true,

    /** Whitelisted roots of this share, stored as JSON. */
    var data: List<ShareRoot> = emptyList(),

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) {
    val isExpired: Boolean
        get() = expiresAt?.let { it <= TimeHelper.now() } ?: false

    val isActive: Boolean
        get() = !isExpired
}
