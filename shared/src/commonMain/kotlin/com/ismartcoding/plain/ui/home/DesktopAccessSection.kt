package com.ismartcoding.plain.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.sessions.onlineClientIds
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.access_settings
import com.ismartcoding.plain.i18n.desktop_access_desc
import com.ismartcoding.plain.i18n.desktop_access_instructions
import com.ismartcoding.plain.i18n.home_remote_access
import com.ismartcoding.plain.platform.system.setDesktopAccessEnabled
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.settings.DesktopAccessSettingsViewModel
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.web.components.WebAddressPager
import kotlinx.coroutines.flow.map
import com.ismartcoding.plain.ui.theme.plainColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopAccessSection(navController: NavHostController) {
    val desktopAccessEnabled = Prefs.desktopAccess.collectAsStateValue()
    val scope = rememberCoroutineScope()
    val onlineCount by onlineClientIds.map { it.size }.collectAsState(0)

    Column {
        PListItem(
            title = { Text(stringResource(Res.string.home_remote_access), style = MaterialTheme.typography.headlineSmall) },
            subtitle = {
                Text(stringResource(if (desktopAccessEnabled) Res.string.desktop_access_instructions else Res.string.desktop_access_desc))
            },
        ) {
            Switch(colors = SwitchDefaults.plainColors(), checked = desktopAccessEnabled, onCheckedChange = { enable ->
                scope.launchSafe { setDesktopAccessEnabled(enable) }
            })
        }
        if (desktopAccessEnabled) {
            val webVM: DesktopAccessSettingsViewModel = viewModel { DesktopAccessSettingsViewModel() }
            val lanIpv4Addresses by webVM.lanIpv4Addresses.collectAsState()
            LaunchedEffect(webVM) { webVM.observeNetworkAddresses() }
            lanIpv4Addresses?.let {
                WebAddressPager(
                    lanIpv4Addresses = it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    compact = true,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (desktopAccessEnabled && onlineCount > 0) {
                    OnlineSessionsIndicator(
                        count = onlineCount,
                        onClick = { navController.navigate(Routing.Connections) },
                    )
                }
            }
            PTextButton(
                text = stringResource(Res.string.access_settings),
                onClick = { navController.navigate(Routing.DesktopAccessSettings) },
            )
        }
    }
}
