package com.ismartcoding.plain.platform

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import com.ismartcoding.plain.AndroidTempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.helpers.NotificationsHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.services.PNotificationListenerService

actual fun toggleNotificationListener(enabled: Boolean) {
    PNotificationListenerService.toggle(appContext, enabled)
}

actual suspend fun getNotificationApp(packageName: String): DNotificationApp? = withIO {
    runCatching {
        val pkg = PackageHelper.getPackage(packageName)
        DNotificationApp(id = pkg.id, name = pkg.name)
    }.getOrNull()
}

actual suspend fun getAllNotificationApps(): List<DNotificationApp> = withIO {
    val ownPackage = appContext.packageName
    PackageHelper.searchAsync("", Int.MAX_VALUE, 0, FileSortBy.NAME_ASC)
        .filter { it.id != ownPackage }
        .map { DNotificationApp(id = it.id, name = it.name) }
}

actual suspend fun filterNotificationsAsync(): List<DNotification> = withIO {
    NotificationsHelper.filterNotificationsAsync(appContext)
}

actual fun replyNotification(id: String, actionIndex: Int, text: String): Boolean {
    val actions = AndroidTempData.notificationActions[id] ?: return false
    // Only consider reply-capable actions (those with remoteInputs)
    val replyActions = actions.filter { it.remoteInputs != null && it.remoteInputs.isNotEmpty() }
    val action = replyActions.getOrNull(actionIndex) ?: return false
    val remoteInputs = action.remoteInputs ?: return false
    val remoteInput = remoteInputs.first()
    val intent = Intent()
    val bundle = Bundle()
    bundle.putCharSequence(remoteInput.resultKey, text)
    RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
    action.actionIntent.send(appContext, 0, intent)
    return true
}

actual fun sendWebLoginNotification(
    browserName: String,
    browserVersion: String,
    osName: String,
    osVersion: String,
    clientIp: String,
) {
    NotificationHelper.sendWebLoginNotification(
        appContext,
        browserName,
        browserVersion,
        osName,
        osVersion,
        clientIp,
    )
}

actual fun generateNotificationId(): Int = NotificationHelper.generateId()

actual fun sendChatMessageNotification(targetId: String, targetName: String, messageText: String) {
    NotificationHelper.sendChatMessageNotification(
        context = appContext,
        targetId = targetId,
        targetName = targetName,
        messageText = messageText,
    )
}
