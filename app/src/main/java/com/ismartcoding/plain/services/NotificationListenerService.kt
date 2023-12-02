package com.ismartcoding.plain.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.extensions.toDNotification

class NotificationListenerService : NotificationListenerService() {
    var isConnected = false
        private set

    private fun isValidNotification(statusBarNotification: StatusBarNotification): Boolean {
        val notification = statusBarNotification.notification
        if (notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0 || notification.flags and Notification.FLAG_ONGOING_EVENT != 0 || notification.flags and Notification.FLAG_LOCAL_ONLY != 0 || notification.flags and NotificationCompat.FLAG_GROUP_SUMMARY != 0 //The notification that groups other notifications
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
            if (!TempData.notifications.any { it.id == n.id }) {
                TempData.notifications.add(n)
            }
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        if (isValidNotification(statusBarNotification)) {
            TempData.notifications.removeIf { it.id == statusBarNotification.key }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        LogCat.d("NotificationListenerService: onListenerConnected")
        val notifications = activeNotifications
        if (notifications != null) {
            for (notification in notifications) {
                if (isValidNotification(notification)) {
                    val n = notification.toDNotification(applicationContext)
                    TempData.notifications.add(n)
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        LogCat.d("NotificationListenerService: onListenerDisconnected")
    }
}

