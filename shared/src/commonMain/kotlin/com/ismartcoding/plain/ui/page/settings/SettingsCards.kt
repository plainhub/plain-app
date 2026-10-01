package com.ismartcoding.plain.ui.page.settings

import com.ismartcoding.plain.preferences.*
import androidx.compose.foundation.layout.padding
import com.ismartcoding.plain.ui.theme.PlainTheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.allow_any_host
import com.ismartcoding.plain.i18n.allow_any_host_desc
import com.ismartcoding.plain.i18n.ble_debug
import com.ismartcoding.plain.ui.resources.circle_alert as ui_drawable_circle_alert
import com.ismartcoding.plain.i18n.client_id
import com.ismartcoding.plain.i18n.developer_mode
import com.ismartcoding.plain.ui.resources.layout_grid as ui_drawable_layout_grid
import com.ismartcoding.plain.i18n.mdns_debug
import com.ismartcoding.plain.i18n.not_supported
import com.ismartcoding.plain.i18n.service_debug
import com.ismartcoding.plain.i18n.simulate_crash
import com.ismartcoding.plain.i18n.simulate_crash_desc
import com.ismartcoding.plain.i18n.supported
import com.ismartcoding.plain.i18n.ui_components
import com.ismartcoding.plain.i18n.wifi_aware_debug
import com.ismartcoding.plain.platform.isBluetoothSupported
import com.ismartcoding.plain.platform.isDebugBuild
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.platform.restartServer
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun DeveloperSettingsCard(
    navController: NavHostController,
) {
    val scope = rememberCoroutineScope()
    val developerMode = UserPrefs.developerMode.collectAsStateValue()
    val allowAnyHost = UserPrefs.allowAnyHost.collectAsStateValue()

    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(title = stringResource(Res.string.client_id), value = TempData.clientId)
        PListItem(title = stringResource(Res.string.developer_mode)) {
            PSwitch(activated = developerMode) {
                scope.launch(Dispatchers.Default) {
                    UserPrefs.developerMode.value = it
                }
            }
            HorizontalSpace(8.dp)
        }
        if (developerMode) {
            PListItem(
                modifier = Modifier.clickable { navController.navigate(Routing.WifiAwareDebug) },
                title = stringResource(Res.string.wifi_aware_debug),
                value = stringResource(
                    if (isWifiAwareSupported) Res.string.supported
                    else Res.string.not_supported
                ),
                showMore = true,
            )
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
                    PSwitch(activated = allowAnyHost) {
                        scope.launch(Dispatchers.Default) {
                            UserPrefs.allowAnyHost.value = it
                            restartServer()
                        }
                    }
                    HorizontalSpace(8.dp)
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
