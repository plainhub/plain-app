package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * Singleton entry point that Swift calls after an iOS permission request
 * completes. Exposed to Swift via the PlainShared framework header.
 *
 * Swift's [IosPermissionChecker.requestPermission] is asynchronous — the
 * system permission dialog may take seconds before the user responds. When
 * it does, Swift calls [onPermissionResult] with the final granted state.
 *
 * This object then:
 *  1. Sends [PermissionsResultEvent] so UI composables re-check
 *     [Permission.isGranted] and update their state.
 *  2. If the permission was denied, shows a confirm dialog offering to
 *     open the iOS Settings app via [IosPlatformRegistry.openAppSettings].
 */
object IosPermissionCallback {

    fun onPermissionResult(permissionName: String, granted: Boolean) {
        LogCat.d("IosPermissionCallback: $permissionName granted=$granted")
        val permission = runCatching { Permission.valueOf(permissionName) }.getOrNull()
        val key = permission?.toSysPermission() ?: permissionName
        sendEvent(PermissionsResultEvent(mapOf(key to granted)))
        if (!granted) {
            showPermissionDeniedDialog { IosPlatformRegistry.openAppSettings() }
        }
    }
}
