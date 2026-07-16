package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DNotification

/**
 * Enable or disable the platform notification listener service used by the
 * web console to mirror device notifications.
 * No-op on iOS (no equivalent API).
 */
expect fun toggleNotificationListener(enabled: Boolean)

/**
 * Look up a single installed app by [packageName] for notification-filter UI.
 * Returns null if the app is not installed.
 */
expect suspend fun getNotificationApp(packageName: String): DNotificationApp?

/**
 * List all installable apps (excluding the calling app itself) suitable for
 * the notification-filter selection sheet.
 */
expect suspend fun getAllNotificationApps(): List<DNotificationApp>

/**
 * Filter cached notifications using the platform notification listener state.
 * Returns an empty list on platforms without a notification listener.
 */
expect suspend fun filterNotificationsAsync(): List<DNotification>

/**
 * Reply to a previously posted notification action identified by [id] and
 * [actionIndex] with the given [text]. Returns true if the reply was sent,
 * false if the action was not found (e.g. notification was removed).
 */
expect fun replyNotification(id: String, actionIndex: Int, text: String): Boolean

/**
 * Post a notification announcing a pending web login from a browser.
 * No-op on iOS.
 */
expect fun sendWebLoginNotification(
    browserName: String,
    browserVersion: String,
    osName: String,
    osVersion: String,
    clientIp: String,
)

/**
 * Allocate a fresh platform notification id suitable for foreground services
 * or posted notifications. Always returns a stable value on iOS.
 */
expect fun generateNotificationId(): Int

/**
 * Post a chat message notification to the system notification shade.
 * No-op on platforms without a notification system (iOS).
 *
 * @param targetId chat target identifier (peer id or group id)
 * @param targetName display name shown in the notification title
 * @param messageText message body shown in the notification content
 */
expect fun sendChatMessageNotification(targetId: String, targetName: String, messageText: String)

/**
 * CommonMain lightweight app descriptor used by notification-filter UI.
 * Heavyweight platform-specific app metadata (e.g. Android `ApplicationInfo`)
 * is intentionally kept out of commonMain.
 */
data class DNotificationApp(
    val id: String,
    val name: String,
)
