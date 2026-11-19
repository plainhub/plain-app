package com.ismartcoding.plain.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted

@Composable
internal fun rememberNetworkPermission(permission: Permission): State<Boolean> {
    val granted = remember(permission) { mutableStateOf(permission.isGranted()) }
    LaunchedEffect(permission) {
        Channel.sharedFlow.collect { event ->
            if (event is PermissionsResultEvent && event.map.containsKey(permission.toSysPermission())) {
                granted.value = permission.isGranted()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted.value = permission.isGranted() }
    return granted
}
