package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.ble_debug
import com.ismartcoding.plain.i18n.circle_alert
import com.ismartcoding.plain.i18n.client_id
import com.ismartcoding.plain.i18n.developer_mode
import com.ismartcoding.plain.i18n.layout_grid
import com.ismartcoding.plain.i18n.nearby_share
import com.ismartcoding.plain.i18n.not_supported
import com.ismartcoding.plain.i18n.simulate_crash
import com.ismartcoding.plain.i18n.simulate_crash_desc
import com.ismartcoding.plain.i18n.supported
import com.ismartcoding.plain.i18n.ui_components
import com.ismartcoding.plain.i18n.wifi_aware_debug
import com.ismartcoding.plain.platform.isBluetoothSupported
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.preferences.DeveloperModePreference
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
    var developerMode by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            developerMode = DeveloperModePreference.getAsync()
        }
    }

    PCard {
        PListItem(title = stringResource(Res.string.client_id), value = TempData.clientId)
        PListItem(title = stringResource(Res.string.developer_mode)) {
            PSwitch(activated = developerMode) {
                scope.launch(Dispatchers.Default) {
                    developerMode = it
                    DeveloperModePreference.putAsync(it)
                }
            }
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
                modifier = Modifier.clickable { navController.navigate(Routing.ComponentShowcase) },
                title = stringResource(Res.string.ui_components),
                icon = Res.drawable.layout_grid,
                showMore = true,
            )
            PListItem(
                modifier = Modifier.clickable { throw RuntimeException("Test crash triggered from Developer Options") },
                title = stringResource(Res.string.simulate_crash),
                subtitle = stringResource(Res.string.simulate_crash_desc),
                icon = Res.drawable.circle_alert,
            )
        }
    }
}
