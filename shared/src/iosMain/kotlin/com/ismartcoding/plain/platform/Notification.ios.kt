package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DNotification

actual fun toggleNotificationListener(enabled: Boolean) {}

actual suspend fun getNotificationApp(packageName: String): DNotificationApp? = null

actual suspend fun getAllNotificationApps(): List<DNotificationApp> = emptyList()

actual suspend fun filterNotificationsAsync(): List<DNotification> = emptyList()

actual fun replyNotification(id: String, actionIndex: Int, text: String): Boolean = false

actual fun sendWebLoginNotification(
    browserName: String,
    browserVersion: String,
    osName: String,
    osVersion: String,
    clientIp: String,
) {}

actual fun generateNotificationId(): Int = 1

actual fun sendChatMessageNotification(targetId: String, targetName: String, messageText: String) {}
