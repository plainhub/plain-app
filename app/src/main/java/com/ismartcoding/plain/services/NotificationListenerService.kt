package com.ismartcoding.plain.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.extensions.toDNotification
import com.ismartcoding.plain.features.CancelNotificationsEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import kotlinx.coroutines.Job

class NotificationListenerService : NotificationListenerService() {
    private val events = mutableListOf<Job>()
    var isConnected = false
        private set

    private fun isValidNotification(statusBarNotification: StatusBarNotification): Boolean {
        val notification = statusBarNotification.notification
        if (notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0
            || notification.flags and Notification.FLAG_ONGOING_EVENT != 0
            || notification.flags and Notification.FLAG_LOCAL_ONLY != 0
            || notification.flags and NotificationCompat.FLAG_GROUP_SUMMARY != 0 //The notification that groups other notifications
        ) {
            //This is not a notification we want!
            return false
        }

        val packageName = statusBarNotification.packageName

        if ("com.facebook.orca" == packageName && statusBarNotification.id == 10012 && notification.tickerText == null) {
            //HACK: Hide weird Facebook empty "Messenger" notification that is actually not shown in the phone
            return false
        }

        if (applicationContext.packageName == packageName) {
            // Don't send our own notifications
            return false
        }

        return true
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        if (isValidNotification(statusBarNotification)) {
            val n = statusBarNotification.toDNotification(applicationContext)
            val old = TempData.notifications.find { it.id == n.id }
            if (old != null) {
                TempData.notifications.remove(old)
            }
            TempData.notifications.add(n)
            coMain {
                val context = MainApp.instance
                val enable = withIO { Permission.NOTIFICATION_LISTENER.isEnabledAsync(context) }
                if (enable) {
                    sendEvent(
                        WebSocketEvent(
                            if (old == null) EventType.NOTIFICATION_CREATED else EventType.NOTIFICATION_UPDATED,
                            JsonHelper.jsonEncode(
                                n.toModel()
                            ),
                        )
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        if (isValidNotification(statusBarNotification)) {
            val old = TempData.notifications.find { it.id == statusBarNotification.key }
            if (old != null) {
                TempData.notifications.remove(old)
                sendEvent(
                    WebSocketEvent(
                        EventType.NOTIFICATION_DELETED,
                        JsonHelper.jsonEncode(
                            old.toModel()
                        ),
                    )
                )
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        LogCat.d("NotificationListenerService: onListenerConnected")
        try {
            val notifications = activeNotifications
            if (notifications != null) {
                TempData.notifications.clear()
                for (notification in notifications) {
                    if (isValidNotification(notification)) {
                        val n = notification.toDNotification(applicationContext)
                        TempData.notifications.add(n)
                    }
                }
            }

            events.add(receiveEventHandler<CancelNotificationsEvent> { event ->
                event.ids.forEach {
                    try {
                        cancelNotification(it)
                    } catch (ex: Exception) {
                        LogCat.e(ex.toString())
                    }
                }
            })
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        LogCat.d("NotificationListenerService: onListenerDisconnected")
        events.clear()
    }
}

