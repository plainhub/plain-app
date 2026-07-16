package com.ismartcoding.plain

import android.app.Notification
import com.ismartcoding.plain.data.DNotification

object AndroidTempData {
    val notifications = mutableListOf<DNotification>()

    // Stores notification actions (including RemoteInput reply actions) keyed by notification id
    val notificationActions = mutableMapOf<String, Array<out Notification.Action>>()
}
