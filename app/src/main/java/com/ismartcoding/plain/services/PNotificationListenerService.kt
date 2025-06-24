package com.ismartcoding.plain.services

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.extensions.toDNotification
import com.ismartcoding.plain.events.CancelNotificationsEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.preferences.NotificationFilterPreference
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import kotlinx.coroutines.Job

class PNotificationListenerService : NotificationListenerService() {
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
            val n = statusBarNotification.toDNotification()
            val old = TempData.notifications.find { it.id == n.id }
            if (old != null) {
                TempData.notifications.remove(old)
            }
            TempData.notifications.add(n)
            coIO {
                val enable = Permission.NOTIFICATION_LISTENER.isEnabledAsync(applicationContext)
                if (enable) {
                    val isAllowed = NotificationFilterPreference.isAllowedAsync(applicationContext, statusBarNotification.packageName)
                    if (isAllowed) {
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
        LogCat.d("PNotificationListenerService: onListenerConnected")
        try {
            val notifications = activeNotifications
            if (notifications != null) {
                TempData.notifications.clear()
                for (notification in notifications) {
                    if (isValidNotification(notification)) {
                        TempData.notifications.add(notification.toDNotification())
                    }
                }
            }
        } catch (ex: SecurityException) {
            LogCat.e("SecurityException when getting active notifications: ${ex.message}")
            isConnected = false
            return
        } catch (ex: Exception) {
            LogCat.e("Error getting active notifications: ${ex.message}")
        }

        try {

            events.add(receiveEventHandler<CancelNotificationsEvent> { event ->
                if (!isConnected) {
                    LogCat.w("PNotificationListenerService: not connected, ignoring cancel request")
                    return@receiveEventHandler
                }
                
                try {
                    if (event.ids.size == TempData.notifications.size) {
                        cancelAllNotifications()
                        TempData.notifications.clear()
                    } else {
                        event.ids.forEach { id ->
                            try {
                                cancelNotification(id)
                            } catch (ex: Exception) {
                                LogCat.e("Failed to cancel notification $id: ${ex.message}")
                            }
                        }
                    }
                } catch (ex: SecurityException) {
                    LogCat.e("SecurityException when canceling notifications: ${ex.message}")
                    // Service might have lost permission, mark as disconnected
                    isConnected = false
                } catch (ex: Exception) {
                    LogCat.e("Error canceling notifications: ${ex.message}")
                }
            })
        } catch (ex: Exception) {
            LogCat.e("Error setting up event handlers: ${ex.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        LogCat.d("PNotificationListenerService: onListenerDisconnected")
        try {
            events.forEach { it.cancel() }
            events.clear()
        } catch (ex: Exception) {
            LogCat.e("Error cleaning up events: ${ex.message}")
        }
    }

    companion object {
        fun toggle(context: Context, enable: Boolean) {
            if (enable) {
                val collectorComponent = ComponentName(context, PNotificationListenerService::class.java)
                LogCat.d("ensureCollectorRunning collectorComponent: $collectorComponent")
                var collectorRunning = false
                val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
                if (runningServices == null) {
                    LogCat.d("ensureCollectorRunning() runningServices is NULL")
                    return
                }
                for (service in runningServices) {
                    if (service.service == collectorComponent) {
                        if (service.pid == android.os.Process.myPid()) {
                            collectorRunning = true
                        }
                    }
                }
                if (collectorRunning) {
                    LogCat.d("ensureCollectorRunning: collector is running")
                    return
                }
                LogCat.d("ensureCollectorRunning: collector not running, reviving...")
                val thisComponent = ComponentName(context, PNotificationListenerService::class.java)
                packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            } else {
                val thisComponent = ComponentName(context, PNotificationListenerService::class.java)
                packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }
}

