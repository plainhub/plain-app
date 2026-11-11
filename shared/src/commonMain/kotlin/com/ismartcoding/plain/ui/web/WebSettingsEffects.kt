package com.ismartcoding.plain.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.platform.permissions.requestSystemAccess
import com.ismartcoding.plain.features.permissions.getWebList
import kotlinx.coroutines.CoroutineScope
import com.ismartcoding.plain.ui.state.launchSafe

@Composable
internal fun WebSettingsEffects(
    permissionList: MutableState<List<AccessItem>>,
) {
    LaunchedEffect(Unit) {
        Prefs.apiAccessFeatures.collect { permissionList.value = getWebList() }
    }
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    permissionList.value = getWebList()
                }
                is WindowFocusChangedEvent -> {
                    permissionList.value = getWebList()
                }
            }
        }
    }
}

internal fun toggleAccess(scope: CoroutineScope, m: AccessItem, enable: Boolean) {
    scope.launchSafe {
        if (!enable || m.features.requestSystemAccess()) Prefs.setApiAccess(m.features, enable)
    }
}
