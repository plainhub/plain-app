package com.ismartcoding.plain

import android.app.Notification
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.features.sms.DPendingMms
import kotlin.collections.mutableListOf

object AndroidTempData {
    val notifications = mutableListOf<DNotification>()

    // Stores notification actions (including RemoteInput reply actions) keyed by notification id
    val notificationActions = mutableMapOf<String, Array<out Notification.Action>>()

    /**
     * MMS messages that have been launched in the default SMS app but not yet
     * confirmed as sent.  Exposed through the sms query so the web can show a
     * "sending…" state before and after a page refresh.
     */
    val pendingMmsMessages = mutableListOf<DPendingMms>()
}