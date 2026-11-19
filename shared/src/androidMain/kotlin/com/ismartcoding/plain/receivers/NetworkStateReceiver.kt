package com.ismartcoding.plain.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.notificationManager
import com.ismartcoding.plain.services.HttpServerForegroundService

class NetworkStateReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {

            if (Permission.POST_NOTIFICATIONS.isGranted()) {
                try {
                    val foregroundServiceNotificationId = HttpServerForegroundService.notificationId
                    val existingNotification = notificationManager.activeNotifications.find { it.id == foregroundServiceNotificationId }
                    if (existingNotification != null) {
                        NotificationManagerCompat.from(context).notify(
                            foregroundServiceNotificationId,
                            HttpServerForegroundService.createNotification(context),
                        )
                    }
                } catch (ex: Exception) {
                    LogCat.e(ex.toString())
                }
            }
        }
    }
}
