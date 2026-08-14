package com.ismartcoding.plain

import android.app.Notification
import com.ismartcoding.plain.data.DNotification
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object AndroidTempData {
    // Mutated from NotificationListenerService callbacks, which the system can
    // invoke on different background threads concurrently.
    val notifications = CopyOnWriteArrayList<DNotification>()

    // Stores notification actions (including RemoteInput reply actions) keyed by notification id
    val notificationActions = ConcurrentHashMap<String, Array<out Notification.Action>>()
}
