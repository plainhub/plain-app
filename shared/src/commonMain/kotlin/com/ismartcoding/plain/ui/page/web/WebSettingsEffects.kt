package com.ismartcoding.plain.ui.page.web
import com.ismartcoding.plain.preferences.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationResultEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.PermissionItem
import com.ismartcoding.plain.features.getWebList
import com.ismartcoding.plain.platform.getOwnPackageName
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.isIgnoringBatteryOptimizations
import com.ismartcoding.plain.platform.toggleNotificationListener
import com.ismartcoding.plain.preferences.ApiPermissionsPreference
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun WebSettingsEffects(
    permissionList: MutableState<List<PermissionItem>>,
    shouldIgnoreOptimize: MutableState<Boolean>,
    systemAlertWindow: MutableState<Boolean>,
    notificationListenerGranted: MutableState<Boolean>,
) {
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    permissionList.value = getWebList()
                    systemAlertWindow.value = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                    notificationListenerGranted.value = Permission.NOTIFICATION_LISTENER.isGranted()
                    if (event.map[Permission.NOTIFICATION_LISTENER.toSysPermission()] == true) {
                        toggleNotificationListener(WebPreference.getAsync())
                    }
                }
                is WindowFocusChangedEvent -> {
                    shouldIgnoreOptimize.value = !isIgnoringBatteryOptimizations()
                    notificationListenerGranted.value = Permission.NOTIFICATION_LISTENER.isGranted()
                }
                is IgnoreBatteryOptimizationResultEvent -> {
                    if (shouldIgnoreOptimize.value) {
                        coIO {
                            DialogHelper.showLoading(); delay(1000); DialogHelper.hideLoading()
                            shouldIgnoreOptimize.value = !isIgnoringBatteryOptimizations()
                        }
                    }
                }
            }
        }
    }
}

internal fun togglePermission(scope: CoroutineScope, m: PermissionItem, enable: Boolean) {
    scope.launch {
        ApiPermissionsPreference.putAsync(m.permission, enable)
        if (m.permission == Permission.NOTIFICATION_LISTENER) {
            val webEnabled = WebPreference.getAsync()
            toggleNotificationListener(enable && webEnabled)
        }
        if (enable) {
            val ps = m.permissions.filter { !it.isGranted() }
            if (ps.isNotEmpty()) sendEvent(RequestPermissionsEvent(*ps.toTypedArray()))
        }
    }
}
