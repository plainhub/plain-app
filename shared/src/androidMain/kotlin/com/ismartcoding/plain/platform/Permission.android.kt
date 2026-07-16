package com.ismartcoding.plain.platform

import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.lib.extensions.hasPermission
import org.jetbrains.compose.resources.StringResource

private val pNotificationListenerServiceClass: Class<*> by lazy {
    Class.forName("com.ismartcoding.plain.services.PNotificationListenerService")
}

actual fun isPermissionGranted(perm: String): Boolean {
    val ctx = appContextValue ?: return false
    return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
}

actual fun Permission.isGranted(): Boolean = when {
    this == Permission.QUERY_ALL_PACKAGES -> true
    this == Permission.WRITE_EXTERNAL_STORAGE -> FileHelper.hasStoragePermission(appContext)
    this == Permission.POST_NOTIFICATIONS -> {
        if (isTPlus()) appContext.hasPermission(this.toSysPermission())
        else NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }
    this == Permission.SYSTEM_ALERT_WINDOW -> Settings.canDrawOverlays(appContext)
    this == Permission.NOTIFICATION_LISTENER -> {
        val componentName = ComponentName(appContext, pNotificationListenerServiceClass)
        val enabledListeners = Settings.Secure.getString(appContext.contentResolver, "enabled_notification_listeners")
        enabledListeners?.contains(componentName.flattenToString()) == true
    }
    else -> appContext.hasPermission(this.toSysPermission())
}

actual suspend fun ensureNotificationPermissionAsync(): Boolean =
    Permissions.ensureNotificationAsync(appContext)

actual fun checkNotificationPermission(stringResource: StringResource, onGranted: () -> Unit) {
    Permissions.checkNotification(appContext, stringResource, onGranted)
}
