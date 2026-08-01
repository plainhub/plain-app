package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.lib.channel.receiveEventHandler
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.Job

/**
 * Registers a handler for [RequestPermissionsEvent] on iOS. When the UI
 * (e.g. [com.ismartcoding.plain.ui.base.NeedPermissionColumn]) requests a
 * permission, this handler delegates to Swift via
 * [IosPlatformRegistry.requestPermission].
 *
 * Swift's implementation is asynchronous — the system permission dialog
 * may take seconds before the user responds. When it does, Swift calls
 * [IosPermissionCallback.onPermissionResult], which sends
 * [com.ismartcoding.plain.events.PermissionsResultEvent] and optionally
 * shows a "go to Settings" dialog if the permission was denied.
 *
 * Call [register] once at app startup (from [com.ismartcoding.plain.initIosApp]).
 */
object IosPermissionEvents {
    private var eventJob: Job? = null

    fun register() {
        if (eventJob?.isActive == true) return
        eventJob = receiveEventHandler<RequestPermissionsEvent> { event ->
            val permission = event.permissions.first()
            LogCat.d("IosPermissionEvents: requesting ${permission.name}")
            IosPlatformRegistry.requestPermission(permission.name)
        }
    }
}
