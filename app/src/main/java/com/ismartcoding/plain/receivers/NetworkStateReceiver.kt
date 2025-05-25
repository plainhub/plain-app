package com.ismartcoding.plain.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.notificationManager
import com.ismartcoding.plain.web.HttpServerManager

class NetworkStateReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            
            if (Permission.POST_NOTIFICATIONS.can(context)) {
                try {
                    val notificationId = HttpServerManager.notificationId
                    val existingNotification = notificationManager.activeNotifications.find { it.id == notificationId }
                    if (existingNotification != null) {
                        NotificationManagerCompat.from(context).notify(
                            notificationId, NotificationHelper.createServiceNotification(
                                context,
                                "${BuildConfig.APPLICATION_ID}.action.stop_http_server",
                                LocaleHelper.getString(R.string.api_service_is_running),
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