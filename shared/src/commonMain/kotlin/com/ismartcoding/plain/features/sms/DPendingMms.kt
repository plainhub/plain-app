package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.TempData
import kotlin.time.Instant

/**
 * Temporary in-memory record of an MMS that has been handed off to the
 * default SMS app but whose delivery has not yet been confirmed.
 *
 * It is stored in [TempData.pendingMmsMessages] and exposed through the
 * `sms` GraphQL query so the web can display a "sending…" placeholder
 * even across a page refresh.  The entry is removed (and attachment
 * files deleted) once the polling in AppEvents detects the sent row in
 * `content://mms`.
 */
data class DPendingMms(
    val id: String,                        // e.g. "pending_mms_<timestampMs>"
    val number: String,
    val body: String,
    val attachments: List<DMessageAttachment>,
    val threadId: String,
    val launchTimeSec: Long,               // epoch-seconds used for the poll query
    val createdAt: Instant,
)