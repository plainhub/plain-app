package com.ismartcoding.plain.helpers

import android.content.Context
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.preferences.NotificationFilterPreference

object NotificationsHelper {
    suspend fun filterNotificationsAsync(context: Context): List<DNotification> {
        val filterData = NotificationFilterPreference.getValueAsync(context)
        val filteredNotifications = mutableListOf<DNotification>()
        for (notification in TempData.notifications) {
            // Apply filter logic directly without async call
            val isAllowed = when (filterData.mode) {
                "allowlist" -> filterData.apps.contains(notification.appId)
                "blacklist" -> !filterData.apps.contains(notification.appId)
                else -> true
            }

            if (isAllowed) {
                filteredNotifications.add(notification)
            }
        }
        return filteredNotifications
    }
}