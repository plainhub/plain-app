package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.platform.isTPlus
import com.ismartcoding.plain.platform.system.ShizukuHelper

import android.content.ComponentName
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.mainActivity
import com.ismartcoding.plain.ui.permissions.Permissions
import com.ismartcoding.plain.lib.extensions.hasPermission
import android.Manifest
import android.os.Environment
import com.ismartcoding.plain.platform.isRPlus

private val pNotificationListenerServiceClass: Class<*> by lazy {
    Class.forName("com.ismartcoding.plain.services.PNotificationListenerService")
}

actual fun Permission.isGranted(): Boolean = when {
    this == Permission.QUERY_ALL_PACKAGES -> true
    this == Permission.ADB -> ShizukuHelper.isGranted()
    // Clipboard access needs no OS runtime permission; the web-side switch
    // (Prefs.apiPermissions) is the sole gate.
    this == Permission.CLIPBOARD -> true
    this == Permission.WRITE_EXTERNAL_STORAGE -> if (isRPlus()) {
        Environment.isExternalStorageManager()
    } else {
        appContext.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
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

// A true value means "denied once, dialog still possible". False after a
// permanent ("don't ask again") denial — but also false when never asked, so
// only consult it after an actual denial.
actual fun Permission.shouldShowRationale(): Boolean {
    val activity = mainActivity ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, this.toSysPermission())
}

actual suspend fun ensureNotificationPermissionAsync(): Boolean =
    Permissions.ensureNotificationAsync(appContext)


actual fun imageLibraryPermission(): FeaturePermission = FeaturePermission(setOf(Permission.WRITE_EXTERNAL_STORAGE), Permission.WRITE_EXTERNAL_STORAGE)
