package com.ismartcoding.plain.ui.page.home
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
import com.ismartcoding.plain.i18n.devices
import com.ismartcoding.plain.i18n.open_web_address
import com.ismartcoding.plain.preferences.DesktopAccessPreference
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDivider
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddressPager
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.launchSafe
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.tipsText
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.setOnlineClientIds
import com.ismartcoding.plain.httpserver.onlineClientIds
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopAccessSection(navController: NavHostController) {
    val desktopAccessEnabled = TempData.desktopAccessEnabled.collectAsStateValue()
    val scope = rememberCoroutineScope()
    val onlineCount by onlineClientIds.map { it.size }.collectAsState(0)

    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(icon = Res.drawable.devices, title = stringResource(Res.string.desktop_access)) {
            PSwitch(activated = desktopAccessEnabled) { enable ->
                scope.launchSafe {
                    DesktopAccessPreference.putAsync(enable)
                    if (!enable) {
                        // Desktop access disabled: actively close all live WebSocket
                        // sessions so browsers stop talking to a disabled endpoint.
                        HttpServerManager.wsSessions.toList().forEach { it.close() }
                        HttpServerManager.wsSessions.clear()
                        setOnlineClientIds(emptySet())
                    }
                }
            }
            HorizontalSpace(8.dp)
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
