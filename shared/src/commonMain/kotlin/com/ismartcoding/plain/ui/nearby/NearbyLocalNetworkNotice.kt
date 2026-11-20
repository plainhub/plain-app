package com.ismartcoding.plain.ui.nearby

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.shouldShowRationale
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PAlertTextButton
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NearbyLocalNetworkNotice(permission: Permission) {
    var settingsRequired by remember(permission) { mutableStateOf(false) }
    LaunchedEffect(permission) {
        Channel.sharedFlow.collect { event ->
            if (event is PermissionsResultEvent && event.map[permission.toSysPermission()] == false) {
                settingsRequired = !permission.shouldShowRationale()
            }
        }
    }
    PAlert(
        description = stringResource(Res.string.nearby_lan_permission_hint),
        type = AlertType.WARNING,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PAlertTextButton(
            text = stringResource(if (settingsRequired) Res.string.open_settings else Res.string.nearby_lan_allow),
            onClick = {
                if (settingsRequired) openAppSettings() else sendEvent(RequestPermissionsEvent(permission))
            },
        )
    }
}
