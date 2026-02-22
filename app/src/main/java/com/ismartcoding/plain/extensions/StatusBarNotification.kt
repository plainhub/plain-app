package com.ismartcoding.plain.extensions

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.ismartcoding.lib.extensions.getString2
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.features.PackageHelper
import kotlin.time.Instant

fun StatusBarNotification.toDNotification(): DNotification {
    val appName = PackageHelper.getLabel(packageName)
    val title = notification.extras.getString2(Notification.EXTRA_TITLE)
    val text = notification.extras.getString2(Notification.EXTRA_TEXT)
    val actions = mutableListOf<String>()
    val replyActions = mutableListOf<String>()

    if (notification.actions != null) {
        for (action in notification.actions) {
            if (action.title == null) {
                continue
            }

            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                // Reply action - collect separately
                replyActions.add(action.title.toString())
            } else {
                actions.add(action.title.toString())
            }
        }
    }

    return DNotification(
        id = key,
        onlyOnce = notification.flags and NotificationCompat.FLAG_ONLY_ALERT_ONCE != 0,
        isClearable = isClearable,
        appId = packageName,
        appName = appName.ifEmpty { packageName },
        time = Instant.fromEpochMilliseconds(postTime),
        silent = notification.flags and Notification.FLAG_INSISTENT != 0,
        title = title,
        body = text,
        actions = actions,
        replyActions = replyActions
    )
}