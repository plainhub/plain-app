package com.ismartcoding.plain.services

import com.ismartcoding.plain.rust.native.RustHostReports
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.isDebugBuild
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.Notification as AppNotification
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.HttpServerResources
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.extensions.toAppNotification
import com.ismartcoding.plain.features.notifications.NotificationChange
import com.ismartcoding.plain.features.notifications.RustNotificationEvents
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class PNotificationListenerService : NotificationListenerService() {
    @Volatile
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

        if (applicationContext.packageName == packageName && !isDebugBuild()) {
            // Don't send our own notifications (allow in DEBUG for testing)
            return false
        }

        return true
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        if (!isConnected || !HttpServerResources.shouldEnableNotificationListener) return
        if (isValidNotification(statusBarNotification)) {
            val n = statusBarNotification.toAppNotification()
            val old = notifications.find { it.id == n.id }
            if (old != null) {
                notifications.remove(old)
            }
            notifications.add(n)
            // Store raw actions for reply support
            val rawActions = statusBarNotification.notification.actions
            if (rawActions != null) {
                notificationActions[n.id] = rawActions
            } else {
                notificationActions.remove(n.id)
            }
            coIO { RustNotificationEvents.publish(if (old == null) NotificationChange.CREATED else NotificationChange.UPDATED, n) }

        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        if (!isConnected || !HttpServerResources.shouldEnableNotificationListener) return
        if (isValidNotification(statusBarNotification)) {
            val old = notifications.find { it.id == statusBarNotification.key }
            if (old != null) {
                notifications.remove(old)
                notificationActions.remove(old.id)
                coIO { RustNotificationEvents.publish(NotificationChange.DELETED, old) }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (!HttpServerResources.shouldEnableNotificationListener) {
            stopListening()
            return
        }
        isConnected = true
        connectedService = this
        LogCat.d("PNotificationListenerService: onListenerConnected")
        try {
            val active = activeNotifications
            if (active != null) {
                notifications.clear()
                notificationActions.clear()
                for (notification in active) {
                    if (isValidNotification(notification)) {
                        val n = notification.toAppNotification()
                        notifications.add(n)
                        val rawActions = notification.notification.actions
                        if (rawActions != null) {
                            notificationActions[n.id] = rawActions
                        }
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

        // The system binds this service as soon as notification access is
        // granted — including grants made directly in Android settings with no
        // in-app toggle involved, so no PermissionsResultEvent ever fires. Push
        // the fresh snapshot so web clients drop their no-permission state.
        coIO {
            RustHostReports.permissions("true")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        if (connectedService === this) connectedService = null
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        if (connectedService === this) connectedService = null
        LogCat.d("PNotificationListenerService: onListenerDisconnected")
        if (!HttpServerResources.shouldEnableNotificationListener || !Permission.NOTIFICATION_LISTENER.isGranted()) return
        try {
            requestRebind(ComponentName(applicationContext, PNotificationListenerService::class.java))
        } catch (ex: Exception) {
            LogCat.e("Error requesting rebind: ${ex.message}")
        }
    }

    private fun stopListening() {
        requestUnbind()
        isConnected = false
        if (connectedService === this) connectedService = null
    }

    companion object {
        internal val notifications = CopyOnWriteArrayList<AppNotification>()
        internal val notificationActions = ConcurrentHashMap<String, Array<out Notification.Action>>()

        @Volatile
        internal var connectedService: PNotificationListenerService? = null

        fun preparePermissionRequest(context: Context) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, PNotificationListenerService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

        fun toggle(context: Context, enable: Boolean) {
            if (!AppFeatureType.NOTIFICATIONS.has()) {
                return
            }
            if (enable) {
                preparePermissionRequest(context)
                if (connectedService?.isConnected != true && Permission.NOTIFICATION_LISTENER.isGranted()) {
                    NotificationListenerService.requestRebind(ComponentName(context, PNotificationListenerService::class.java))
                }
            } else {
                connectedService?.stopListening()
            }
        }
    }
}
