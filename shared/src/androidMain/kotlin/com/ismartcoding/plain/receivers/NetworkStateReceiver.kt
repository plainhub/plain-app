package com.ismartcoding.plain.receivers

import com.ismartcoding.plain.i18n.*

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.notificationManager
import com.ismartcoding.plain.web.HttpServerManager

class NetworkStateReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            
            if (Permission.POST_NOTIFICATIONS.isGranted()) {
                try {
                    val notificationId = HttpServerManager.notificationId
                    val existingNotification = notificationManager.activeNotifications.find { it.id == notificationId }
                    if (existingNotification != null) {
                        NotificationManagerCompat.from(context).notify(
                            notificationId, NotificationHelper.createServiceNotification(
                                context,
                                AppIntents.ACTION_STOP_HTTP_SERVER,
                                LocaleHelper.getString(Res.string.api_service_is_running),
                                HttpServerManager.getNotificationContent()
                            )
                        )
                    }
                } catch (ex: Exception) {
                    LogCat.e(ex.toString())
                }
            }
        }
    }
}