package com.ismartcoding.plain

import android.app.Notification
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.data.DNotification

object AndroidTempData {
    // Mutated from NotificationListenerService callbacks, which the system can
    // invoke on different background threads concurrently, so plain
    // mutableListOf/mutableMapOf (ArrayList/LinkedHashMap) are not safe here.
    val notifications = mutableStateListOf<DNotification>()

    // Stores notification actions (including RemoteInput reply actions) keyed by notification id
    val notificationActions = mutableStateMapOf<String, Array<out Notification.Action>>()
}
