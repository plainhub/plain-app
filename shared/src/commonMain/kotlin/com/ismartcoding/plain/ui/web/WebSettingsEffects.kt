package com.ismartcoding.plain.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.ui.permissions.PermissionItem
import com.ismartcoding.plain.features.permissions.getWebList
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.permissions.isGranted
import kotlinx.coroutines.CoroutineScope
import com.ismartcoding.plain.ui.state.launchSafe

@Composable
internal fun WebSettingsEffects(
    permissionList: MutableState<List<PermissionItem>>,
    systemAlertWindow: MutableState<Boolean>,
    notificationListenerGranted: MutableState<Boolean>,
) {
    LaunchedEffect(Unit) {
        Prefs.apiPermissions.collect { permissionList.value = getWebList() }
    }
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    permissionList.value = getWebList()
                    systemAlertWindow.value = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                    notificationListenerGranted.value = Permission.NOTIFICATION_LISTENER.isGranted()
                }
                is WindowFocusChangedEvent -> {
                    notificationListenerGranted.value = Permission.NOTIFICATION_LISTENER.isGranted()
                }
            }
        }
    }
}

internal fun togglePermission(scope: CoroutineScope, m: PermissionItem, enable: Boolean) {
    scope.launchSafe {
        Prefs.setApiPermission(m.permission, enable)
        if (enable) {
            val ps = m.permissions.filter { !it.isGranted() }
            if (ps.isNotEmpty()) sendEvent(RequestPermissionsEvent(*ps.toTypedArray()))
        }
    }
}
