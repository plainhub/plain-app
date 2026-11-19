package com.ismartcoding.plain.ui.platform.notifications

import com.ismartcoding.plain.ui.i18n.LocaleHelper

import com.ismartcoding.plain.i18n.*

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.compose.ui.graphics.toArgb
import com.ismartcoding.plain.lib.extensions.notificationManager
import com.ismartcoding.plain.lib.extensions.hasPermission
import com.ismartcoding.plain.platform.isSPlus
import com.ismartcoding.plain.platform.isTPlus
import com.ismartcoding.plain.ui.theme.BrandPrimary
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.app.AppIntents
import com.ismartcoding.plain.app.IntentExtras
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.receivers.PeerChatReplyReceiver
import com.ismartcoding.plain.receivers.ServiceStopBroadcastReceiver

private val mainActivityClass: Class<*> by lazy {
    Class.forName("com.ismartcoding.plain.MainActivity")
}

private fun notificationDrawableId(): Int {
    return appContext.resources.getIdentifier("notification", "drawable", appContext.packageName)
}

object NotificationHelper {
    private fun createContentIntent(
        context: Context,
        chatTargetId: String? = null,
        requestCode: Int = 0,
    ): PendingIntent {
        val intent = Intent(context, mainActivityClass).apply {
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            if (!chatTargetId.isNullOrEmpty()) {
                putExtra(IntentExtras.CHAT_TARGET_ID, chatTargetId)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun generateId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

    fun ensureDefaultChannel() {
        val notificationManager = appContext.notificationManager
        if (notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    LocaleHelper.getString(Res.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    setShowBadge(false)
                },
            )
        }
    }

    fun ensureChatChannel() {
        val notificationManager = appContext.notificationManager
        if (notificationManager.getNotificationChannel(Constants.CHAT_NOTIFICATION_CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    Constants.CHAT_NOTIFICATION_CHANNEL_ID,
                    LocaleHelper.getString(Res.string.peer_chat),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendChatMessageNotification(context: Context, targetId: String, targetName: String, messageText: String) {
        val notificationsEnabled = if (isTPlus()) {
            context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        if (!notificationsEnabled) return
        ensureChatChannel()

        val notificationId = ("chat_$targetId").hashCode()

        val replyIntent = Intent(context, PeerChatReplyReceiver::class.java).apply {
            `package` = context.packageName
            action = AppIntents.ACTION_PEER_CHAT_REPLY
            putExtra(PeerChatReplyReceiver.EXTRA_TARGET_ID, targetId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val remoteInput = RemoteInput.Builder(PeerChatReplyReceiver.KEY_TEXT_REPLY)
            .setLabel(LocaleHelper.getString(Res.string.peer_chat_type_reply))
            .build()

        val replyAction = NotificationCompat.Action.Builder(
            notificationDrawableId(),
            LocaleHelper.getString(Res.string.peer_chat_reply),
            replyPendingIntent,
        )
            .addRemoteInput(remoteInput)
            .build()

        val notification = NotificationCompat.Builder(context, Constants.CHAT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(notificationDrawableId())
            .setColor(BrandPrimary.toArgb())
            .setContentTitle(targetName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent(context, targetId, notificationId))
            .addAction(replyAction)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun postWebLoginNotification(context: Context, title: String, description: String) {
        if (!Permission.POST_NOTIFICATIONS.isGranted()) return
        ensureDefaultChannel()
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(notificationDrawableId())
            .setColor(BrandPrimary.toArgb())
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createContentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(generateId(), notification)
    }

    fun createServiceNotification(
        context: Context,
        title: String,
        description: String = "",
        stopAction: String? = null,
    ): Notification {
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(notificationDrawableId())
            setColor(BrandPrimary.toArgb())
            setContentTitle(title)
            setContentText(description)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOnlyAlertOnce(true)
            setSilent(true)
            setWhen(System.currentTimeMillis())
            setAutoCancel(false)
            setOngoing(true)
            if (isSPlus()) {
                // https://issuetracker.google.com/issues/229000935
                foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            }
            setContentIntent(createContentIntent(context))
            if (stopAction != null) {
                val stopIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, ServiceStopBroadcastReceiver::class.java).setAction(stopAction),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                addAction(-1, LocaleHelper.getString(Res.string.stop_service), stopIntent)
            } else {
                val repostIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    Intent(context, ServiceStopBroadcastReceiver::class.java)
                        .setAction(AppIntents.ACTION_REPOST_BACKGROUND_NOTIFICATION),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                setDeleteIntent(repostIntent)
            }
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }.build()
    }
}
