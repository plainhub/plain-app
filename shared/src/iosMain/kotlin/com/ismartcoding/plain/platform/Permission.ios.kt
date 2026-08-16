package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration.Companion.seconds

actual fun isPermissionGranted(perm: String): Boolean = true

actual fun Permission.isGranted(): Boolean = when (this) {
    Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEOS, Permission.READ_MEDIA_AUDIO,
    Permission.READ_CONTACTS, Permission.WRITE_CONTACTS,
    Permission.ACCESS_FINE_LOCATION,
    Permission.CAMERA, Permission.RECORD_AUDIO,
    Permission.POST_NOTIFICATIONS,
    -> IosPlatformRegistry.isPermissionGranted(this.name)
    else -> true
}

actual suspend fun ensureNotificationPermissionAsync(): Boolean {
    if (IosPlatformRegistry.isPermissionGranted(Permission.POST_NOTIFICATIONS.name)) {
        return true
    }
    IosPlatformRegistry.requestPermission(Permission.POST_NOTIFICATIONS.name)
    val event = withTimeoutOrNull(60.seconds) {
        Channel.sharedFlow
            .filterIsInstance<PermissionsResultEvent>()
            .filter { it.has(Permission.POST_NOTIFICATIONS) }
            .first()
    }
    if (event == null) {
        LogCat.w("ensureNotificationPermissionAsync: timed out waiting for POST_NOTIFICATIONS result")
    }
    return Permission.POST_NOTIFICATIONS.isGranted()
}

actual fun checkNotificationPermission(stringResource: StringResource, onGranted: () -> Unit) {
    if (IosPlatformRegistry.isPermissionGranted(Permission.POST_NOTIFICATIONS.name)) {
        onGranted()
    } else {
        showNotificationConfirmDialog(stringResource) {
            coIO {
                ensureNotificationPermissionAsync()
                onGranted()
            }
        }
    }
}
