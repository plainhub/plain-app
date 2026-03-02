package com.ismartcoding.plain

import android.app.Notification
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.sms.DMessageAttachment
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

object TempData {
    var webEnabled = false
    var webHttps = false
    var clientId = ""
    var deviceName = ""
    var httpPort: Int = 8080
    var httpsPort: Int = 8443
    var urlToken = "" // use to encrypt or decrypt params in url
    var mdnsHostname = "plainapp.local" // mDNS hostname for local network discovery
    val notifications = mutableListOf<DNotification>()
    // Stores notification actions (including RemoteInput reply actions) keyed by notification id
    val notificationActions = mutableMapOf<String, Array<out Notification.Action>>()
    var audioPlayMode = MediaPlayMode.REPEAT

    var audioSleepTimerFutureTime = 0L
    var audioPlayPosition = 0L // audio play position in milliseconds

    /**
     * MMS messages that have been launched in the default SMS app but not yet
     * confirmed as sent.  Exposed through the sms query so the web can show a
     * "sending…" state before and after a page refresh.
     */
    val pendingMmsMessages = mutableListOf<DPendingMms>()
}
