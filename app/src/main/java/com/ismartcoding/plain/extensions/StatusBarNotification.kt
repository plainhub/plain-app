package com.ismartcoding.plain.extensions

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.ismartcoding.lib.extensions.getString2
import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.features.PackageHelper
import kotlinx.datetime.Instant

fun StatusBarNotification.toDNotification(context: Context): DNotification {
    val appName = PackageHelper.getLabel(context, packageName)
    val title = notification.extras.getString2(Notification.EXTRA_TITLE)
    val text = notification.extras.getString2(Notification.EXTRA_TEXT)
    val actions = mutableListOf<String>()

    if (notification.actions != null) {
        for (action in notification.actions) {
            if (action.title == null) {
                continue
            }

            // Check whether it is a reply action. We have special treatment for them
            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                continue
            }

            actions.add(action.title.toString())
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
        actions = actions
    )
}