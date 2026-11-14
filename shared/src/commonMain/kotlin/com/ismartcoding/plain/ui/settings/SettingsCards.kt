package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.preferences.*
import androidx.compose.foundation.layout.padding
import com.ismartcoding.plain.ui.theme.PlainTheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.allow_any_host
import com.ismartcoding.plain.i18n.allow_any_host_desc
import com.ismartcoding.plain.i18n.ble_debug
import com.ismartcoding.plain.i18n.client_id
import com.ismartcoding.plain.i18n.mdns_debug
import com.ismartcoding.plain.i18n.not_supported
import com.ismartcoding.plain.i18n.service_debug
import com.ismartcoding.plain.i18n.simulate_crash
import com.ismartcoding.plain.i18n.simulate_crash_desc
import com.ismartcoding.plain.i18n.supported
import com.ismartcoding.plain.i18n.ui_components
import com.ismartcoding.plain.i18n.wifi_aware_debug
import com.ismartcoding.plain.platform.network.isBluetoothSupported
import com.ismartcoding.plain.platform.system.isDebugBuild
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import androidx.compose.material3.Switch
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.navigation.Routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun DeveloperSettingsCard(
    navController: NavHostController,
    developerMode: Boolean,
) {
    val scope = rememberCoroutineScope()
    val allowAnyHost = Prefs.allowAnyHost.collectAsStateValue()

    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(title = stringResource(Res.string.client_id), value = Prefs.clientId.value)
        if (developerMode) {
            if (Capability.WIFI_AWARE in getDeviceCapabilities()) {
                PListItem(
                    modifier = Modifier.clickable { navController.navigate(Routing.WifiAwareDebug) },
                    title = stringResource(Res.string.wifi_aware_debug),
                    value = stringResource(Res.string.supported),
                    showMore = true,
                )
            }
            PListItem(
                modifier = Modifier.clickable { navController.navigate(Routing.BleDebug) },
                title = stringResource(Res.string.ble_debug),
                value = stringResource(
                    if (isBluetoothSupported()) Res.string.supported
                    else Res.string.not_supported
                ),
                showMore = true,
            )
            PListItem(
                modifier = Modifier.clickable { navController.navigate(Routing.ServiceDebug) },
                title = stringResource(Res.string.service_debug),
                showMore = true,
            )
            PListItem(
                modifier = Modifier.clickable { navController.navigate(Routing.MdnsDebug) },
                title = stringResource(Res.string.mdns_debug),
                showMore = true,
            )
            if (isDebugBuild()) {
                PListItem(
                    title = stringResource(Res.string.allow_any_host),
                    subtitle = stringResource(Res.string.allow_any_host_desc),
                ) {
                    Switch(checked = allowAnyHost, onCheckedChange = {
                        scope.launch(Dispatchers.Default) {
                            Prefs.set(PrefKeys.ALLOW_ANY_HOST, it)
                            HttpServerManager.restartHttpServer()
                        }
                    })
                }
                PListItem(
                    modifier = Modifier.clickable { navController.navigate(Routing.ComponentShowcase) },
                    title = stringResource(Res.string.ui_components),
                    showMore = true,
                )
                PListItem(
                    modifier = Modifier.clickable {
                        navController.navigate(Routing.MarkdownThemePreview)
                    },
                    showMore = true,
                    title = "Markdown Preview",
                )
                PListItem(
                    modifier = Modifier.clickable { throw RuntimeException("Test crash triggered from Developer Options") },
                    title = stringResource(Res.string.simulate_crash),
                    subtitle = stringResource(Res.string.simulate_crash_desc),
                )
            }
        }
    }
}
