package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.preferences.*
import com.ismartcoding.plain.ui.theme.PlainTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.access_settings
import com.ismartcoding.plain.i18n.desktop_access
import com.ismartcoding.plain.i18n.desktop_access_desc
import com.ismartcoding.plain.ui.resources.devices as ui_drawable_devices
import com.ismartcoding.plain.i18n.open_web_address
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDivider
import com.ismartcoding.plain.ui.base.PListItem
import androidx.compose.material3.Switch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddressPager
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.launchSafe
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.tipsText
import com.ismartcoding.plain.platform.HttpServerManager
import com.ismartcoding.plain.features.session.onlineClientIds
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.i18n.devices

@Composable
fun DesktopAccessSection(navController: NavHostController) {
    val desktopAccessEnabled = UserPrefs.desktopAccess.collectAsStateValue()
    val scope = rememberCoroutineScope()
    val onlineCount by onlineClientIds.map { it.size }.collectAsState(0)

    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(icon = UiRes.drawable.ui_drawable_devices, title = stringResource(Res.string.desktop_access)) {
            Switch(checked = desktopAccessEnabled, onCheckedChange = { enable ->
                scope.launchSafe {
                    UserPrefs.desktopAccess.value = enable
                    if (!enable) {
                        // Desktop access disabled: actively close all live WebSocket
                        // sessions so browsers stop talking to a disabled endpoint.
                        com.ismartcoding.plain.features.session.closeAllWsSessions()
                    }
                }
            })
        }
        if (desktopAccessEnabled && onlineCount > 0) {
            OnlineSessionsIndicator(
                count = onlineCount,
                onClick = { navController.navigate(Routing.Connections) })
        } else {
            Text(
                text = stringResource(if (desktopAccessEnabled) Res.string.open_web_address else Res.string.desktop_access_desc),
                style = MaterialTheme.typography.tipsText(),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        WebAddressPager()
        VerticalSpace(16.dp)
        PDivider(modifier = Modifier.padding(start = 16.dp))
        PListItem(modifier = Modifier.clickable {
            navController.navigate(Routing.DesktopAccessSettings)
        }, title = stringResource(Res.string.access_settings), showMore = true)
    }
}
