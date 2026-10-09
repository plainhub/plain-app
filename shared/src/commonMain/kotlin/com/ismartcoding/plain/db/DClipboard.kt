package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * A clipboard history entry. [source] is the client id of the origin: empty
 * means captured locally on this device, otherwise received from that peer.
 * [hash] is the SHA-256 of [text] and powers dedup and sync loop suppression.
 * [sensitive] mirrors the platform sensitive flag set by password managers.
 * [label] is an optional user label.
 */
data class DClipboard(
    var id: String,
    var text: String = "",
    var hash: String = "",
    var source: String = "",
    var label: String = "",
    var sensitive: Boolean = false,
    var createdAt: Instant = TimeHelper.now(),
)
