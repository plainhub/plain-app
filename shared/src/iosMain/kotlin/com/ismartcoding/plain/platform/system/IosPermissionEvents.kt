package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.receiveEventHandler
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.ui.permissions.showPermissionDeniedDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription

object IosPermissionEvents {
    private var eventJob: Job? = null

    fun register() {
        if (eventJob?.isActive == true) return
        eventJob = receiveEventHandler<RequestPermissionsEvent> { event ->
            val permission = event.permissions.first()
            LogCat.d("IosPermissionEvents: requesting ${permission.name}")
            Channel.sharedFlow
                .onSubscription { IosPlatformRegistry.requestPermission(permission.name) }
                .filterIsInstance<PermissionsResultEvent>()
                .first { it.has(permission) }
            if (!permission.isGranted() && event.showDeniedDialog) {
                showPermissionDeniedDialog { IosPlatformRegistry.openAppSettings() }
            }
        }
    }
}
