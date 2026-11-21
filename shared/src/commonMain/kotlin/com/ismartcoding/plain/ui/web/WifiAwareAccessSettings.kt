package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.components.PSwitch
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.network.wifiAwarePermission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.permissions.shouldShowRationale
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.permissions.rememberNetworkPermission
import com.ismartcoding.plain.ui.permissions.showPermissionDeniedDialog
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WifiAwareAccessSettings() {
    val permission = remember { wifiAwarePermission() } ?: return
    val granted by rememberNetworkPermission(permission)
    val enabled = Prefs.wifiAwareEnabled.collectAsStateValue()
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toggle: (Boolean) -> Unit = { value ->
        if (!busy) scope.launchSafe {
            busy = true
            try {
                if (value && !permission.isGranted()) {
                    Channel.sharedFlow
                        .onSubscription { sendEvent(RequestPermissionsEvent(permission, showDeniedDialog = false)) }
                        .filterIsInstance<PermissionsResultEvent>()
                        .first { it.has(permission) }
                    if (!permission.isGranted()) {
                        if (!permission.shouldShowRationale()) showPermissionDeniedDialog { openAppSettings() }
                        return@launchSafe
                    }
                }
                Prefs.set(PrefKeys.WIFI_AWARE_ENABLED, value)
            } finally {
                busy = false
            }
        }
    }
    PCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        PListItem(
            title = stringResource(Res.string.wifi_aware_title),
            subtitle = stringResource(Res.string.wifi_aware_transfer_hint),
            modifier = Modifier.clickable(enabled = !busy) { toggle(!(enabled && granted)) },
        ) {
            PSwitch(checked = enabled && granted, enabled = !busy, onCheckedChange = toggle)
        }
    }
}
