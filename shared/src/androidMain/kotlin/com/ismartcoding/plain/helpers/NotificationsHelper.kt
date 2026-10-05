package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.preferences.*
import android.content.Context
import com.ismartcoding.plain.AndroidTempData
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNotification

object NotificationsHelper {
    suspend fun filterNotificationsAsync(context: Context): List<DNotification> {
        return com.ismartcoding.plain.platform.filterNotificationsAsync()
    }
}
