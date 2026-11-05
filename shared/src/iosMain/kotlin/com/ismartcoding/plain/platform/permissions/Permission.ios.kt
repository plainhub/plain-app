package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.platform.system.IosPlatformRegistry

import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

actual fun Permission.isGranted(): Boolean = when (this) {
    Permission.ADB -> false
    Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEOS, Permission.READ_MEDIA_AUDIO,
    Permission.READ_CONTACTS, Permission.WRITE_CONTACTS,
    Permission.ACCESS_FINE_LOCATION,
    Permission.CAMERA, Permission.RECORD_AUDIO,
    Permission.POST_NOTIFICATIONS,
    -> IosPlatformRegistry.isPermissionGranted(this.name)
    else -> true
}

actual fun Permission.shouldShowRationale(): Boolean = false

actual suspend fun ensureNotificationPermissionAsync(): Boolean {
    if (IosPlatformRegistry.isPermissionGranted(Permission.POST_NOTIFICATIONS.name)) {
        return true
    }
    val event = withTimeoutOrNull(60.seconds) {
        Channel.sharedFlow
            .onSubscription { IosPlatformRegistry.requestPermission(Permission.POST_NOTIFICATIONS.name) }
            .filterIsInstance<PermissionsResultEvent>()
            .filter { it.has(Permission.POST_NOTIFICATIONS) }
            .first()
    }
    if (event == null) {
        LogCat.w("ensureNotificationPermissionAsync: timed out waiting for POST_NOTIFICATIONS result")
    }
    return Permission.POST_NOTIFICATIONS.isGranted()
}


actual fun imageLibraryPermission(): FeaturePermission = FeaturePermission(
    com.ismartcoding.plain.features.web.AccessFeature.IMAGES.systemPermissions(), Permission.READ_MEDIA_IMAGES,
)
