package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.features.permissions.getWebList
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.settings.DesktopAccessSettingsViewModel
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAccessSettingsPage(
    navController: NavHostController,
    webVM: DesktopAccessSettingsViewModel = viewModel { DesktopAccessSettingsViewModel() },
    batterySettings: (@Composable () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val enabledFeatures = Prefs.apiAccessFeatures.collectAsStateValue()
    val accessItems = remember { mutableStateOf(getWebList()) }
    val state = webVM.accessSettings
    WebSettingsEffects(accessItems)

    PScaffold(topBar = {
        PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = stringResource(Res.string.access_settings_title))
    }) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            item {
                TopSpace()
                Tips(stringResource(Res.string.access_settings_description), Modifier.padding(horizontal = 16.dp))
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.Connections) },
                        title = stringResource(Res.string.connections), showMore = true,
                    )
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.WebSecurity) },
                        title = stringResource(Res.string.security), showMore = true,
                    )
                }
                VerticalSpace(dp = 16.dp)
                PSectionTitle(title = stringResource(Res.string.access_remote_permissions))
            }
            itemsIndexed(accessItems.value) { index, item ->
                val enabled = item.isEnabled(enabledFeatures)
                val destination = when (item.features.singleOrNull()) {
                    AccessFeature.NOTIFICATIONS -> Routing.NotificationSettings
                    AccessFeature.CLIPBOARD -> Routing.ClipboardHistory
                    else -> null
                }
                val toggle = { scope.launchSafe { state.setEnabled(item.features, !enabled) }; Unit }
                Column(PlainTheme.getCardModifier(index = index, size = accessItems.value.size)) {
                    PListItem(
                        modifier = Modifier.clickable(enabled = !state.busy) {
                            if (destination != null) navController.navigate(destination) else toggle()
                        },
                        title = item.getText(), subtitle = item.getDescription(),
                        separatedActions = destination != null, showMore = destination != null,
                    ) {
                        Switch(checked = enabled, enabled = !state.busy, onCheckedChange = { toggle() })
                    }
                    val denied = item.features.any { it in state.deniedFeatures }
                    if (enabled || denied) {
                        AccessIssueNotice(item, enabled, denied, state.busy) {
                            scope.launchSafe { state.setEnabled(item.features, true) }
                        }
                    }
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    if (Capability.ADB in getDeviceCapabilities()) {
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
                            title = stringResource(Res.string.adb_automation), showMore = true,
                        )
                    }
                    PListItem(
                        modifier = Modifier.clickable { openAppSettings() },
                        title = stringResource(Res.string.open_permission_settings), showMore = true,
                    )
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.HowToUse) },
                        title = stringResource(Res.string.how_to_use), showMore = true,
                    )
                }
            }
            if (batterySettings != null) item { batterySettings() }
            item { BottomSpace(paddingValues) }
        }
    }
}
