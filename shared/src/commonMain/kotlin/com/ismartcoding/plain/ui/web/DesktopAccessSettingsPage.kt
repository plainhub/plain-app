package com.ismartcoding.plain.ui.web


import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.platform.system.openAppSettings
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.features.permissions.getWebList
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import androidx.compose.material3.Switch
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.settings.DesktopAccessSettingsViewModel
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.bell as ui_drawable_bell
import com.ismartcoding.plain.ui.resources.code as ui_drawable_code
import com.ismartcoding.plain.ui.resources.content_paste as ui_drawable_content_paste
import com.ismartcoding.plain.ui.resources.devices as ui_drawable_devices
import com.ismartcoding.plain.ui.resources.info as ui_drawable_info
import com.ismartcoding.plain.ui.resources.lock as ui_drawable_lock

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DesktopAccessSettingsPage(
    navController: NavHostController,
    webVM: DesktopAccessSettingsViewModel = viewModel { DesktopAccessSettingsViewModel() },
    batterySettings: (@Composable () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val enabledFeatures = Prefs.apiAccessFeatures.collectAsStateValue()
    val accessItems = remember { mutableStateOf(getWebList()) }
    val notificationListenerGranted = remember { mutableStateOf(AccessFeature.NOTIFICATIONS.isGranted()) }
    val notificationsCard = AppFeatureType.NOTIFICATIONS.has()
    val listState = rememberLazyListState()
    WebSettingsEffects(accessItems, notificationListenerGranted)

    PScaffold(topBar = {
    PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = stringResource(Res.string.access_settings))
    }, content = { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
        ) {
            item {
                TopSpace()
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.Connections) },
                        icon = UiRes.drawable.ui_drawable_devices, title = stringResource(Res.string.connections), showMore = true
                    )
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.WebSecurity) },
                        icon = UiRes.drawable.ui_drawable_lock, title = stringResource(Res.string.security), showMore = true
                    )
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.HowToUse) },
                        icon = UiRes.drawable.ui_drawable_info, title = stringResource(Res.string.how_to_use), showMore = true
                    )
                }
                VerticalSpace(dp = 16.dp)
            }
            item { PSectionTitle(title = stringResource(Res.string.features)) }
            itemsIndexed(accessItems.value) { index, m ->
                Column(PlainTheme.getCardModifier(index = index, size = accessItems.value.size)) {
                    PListItem(
                        modifier = Modifier.clickable { toggleAccess(scope, m, !m.isEnabled(enabledFeatures)) },
                        icon = m.icon, title = m.getText(),
                    ) {
                        Switch(checked = m.isEnabled(enabledFeatures), onCheckedChange = { enable -> toggleAccess(scope, m, enable) })
                    }
                    if (m.isEnabled(enabledFeatures)) AccessIssueNotice(m)
                }
            }

            if (notificationsCard) {
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        val m = remember(notificationListenerGranted.value) { AccessItem(UiRes.drawable.ui_drawable_bell, setOf(AccessFeature.NOTIFICATIONS)) }
                        val enabled = m.isEnabled(enabledFeatures)
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.NotificationSettings) },
                            icon = m.icon, title = m.getText(),
                            separatedActions = true
                        ) {
                            Switch(checked = enabled, onCheckedChange = { enable -> toggleAccess(scope, m, enable) })
                        }
                        if (enabled) AccessIssueNotice(m)
                    }
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                val m = remember { AccessItem(UiRes.drawable.ui_drawable_content_paste, setOf(com.ismartcoding.plain.features.web.AccessFeature.CLIPBOARD)) }
                val clipboardEnabled = m.isEnabled(enabledFeatures)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.ClipboardHistory) },
                        icon = m.icon, title = stringResource(Res.string.clipboard_sync),
                        separatedActions = true
                    ) {
                        Switch(checked = clipboardEnabled, onCheckedChange = { enable -> toggleAccess(scope, m, enable) })
                    }
                }
                if (clipboardEnabled) {
                    Tips(stringResource(Res.string.clipboard_sync_tips))
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(modifier = Modifier.clickable { openAppSettings() }, title = stringResource(Res.string.open_permission_settings), showMore = true)
                }
            }
            if (batterySettings != null) {
                item {
                    VerticalSpace(dp = 16.dp); PSectionTitle(title = stringResource(Res.string.performance))
                    batterySettings()
                }
            }
            if (com.ismartcoding.plain.platform.system.Capability.ADB in com.ismartcoding.plain.platform.system.getDeviceCapabilities()) {
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
                            icon = UiRes.drawable.ui_drawable_code, title = stringResource(Res.string.adb_automation), showMore = true
                        )
                    }
                }
            }
            item { BottomSpace(paddingValues) }
        }
    })

}
