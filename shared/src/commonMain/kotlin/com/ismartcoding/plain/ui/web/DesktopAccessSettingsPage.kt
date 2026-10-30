package com.ismartcoding.plain.ui.web

import com.ismartcoding.plain.ui.permissions.getText

import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
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
import com.ismartcoding.plain.ui.permissions.PermissionItem
import com.ismartcoding.plain.features.permissions.getWebList
import com.ismartcoding.plain.platform.permissions.Permission
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
    val keepAwake = Prefs.keepAwake.collectAsStateValue()
    val scope = rememberCoroutineScope()
    val enabledPermissions = Prefs.apiPermissions.collectAsStateValue()
    val permissionList = remember { mutableStateOf(getWebList()) }
    val systemAlertWindow = remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.isGranted()) }
    val notificationListenerGranted = remember { mutableStateOf(Permission.NOTIFICATION_LISTENER.isGranted()) }
    val notificationsCard = AppFeatureType.NOTIFICATIONS.has()
    val listState = rememberLazyListState()
    WebSettingsEffects(permissionList, systemAlertWindow, notificationListenerGranted)

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
            itemsIndexed(permissionList.value) { index, m ->
                val permission = m.permission
                PListItem(
                    modifier = PlainTheme.getCardModifier(index = index, size = permissionList.value.size)
                        .clickable { togglePermission(scope, m, !enabledPermissions.contains(permission.name)) },
                    icon = m.icon, title = permission.getText(),
                    subtitle = stringResource(if (m.granted) Res.string.system_permission_granted else Res.string.system_permission_not_granted)
                ) {
                    Switch(checked = enabledPermissions.contains(permission.name), onCheckedChange = { enable ->
                        togglePermission(scope, m, enable)
                    })
                }
            }
            if (notificationsCard) {
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        val m = PermissionItem.create(UiRes.drawable.ui_drawable_bell, Permission.NOTIFICATION_LISTENER)
                        val permission = m.permission
                        val enabled = notificationListenerGranted.value && enabledPermissions.contains(permission.name)
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.NotificationSettings) },
                            icon = m.icon, title = permission.getText(),
                            subtitle = stringResource(if (notificationListenerGranted.value) Res.string.system_permission_granted else Res.string.system_permission_not_granted),
                            separatedActions = true
                        ) {
                            Switch(checked = enabled, onCheckedChange = { enable -> togglePermission(scope, m, enable) })
                        }
                    }
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                val m = remember { PermissionItem.create(UiRes.drawable.ui_drawable_content_paste, Permission.CLIPBOARD) }
                val clipboardEnabled = enabledPermissions.contains(m.permission.name)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.ClipboardHistory) },
                        icon = m.icon, title = stringResource(Res.string.clipboard_sync),
                        separatedActions = true
                    ) {
                        Switch(checked = clipboardEnabled, onCheckedChange = { enable -> togglePermission(scope, m, enable) })
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
            item {
                VerticalSpace(dp = 16.dp); PSectionTitle(title = stringResource(Res.string.performance))
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(modifier = Modifier.clickable { webVM.enableKeepAwake(!keepAwake) }, title = stringResource(Res.string.keep_awake)) {
                        Switch(checked = keepAwake, onCheckedChange = { enable -> webVM.enableKeepAwake(enable) })
                    }
                }
                Tips(stringResource(Res.string.keep_awake_tips))
                batterySettings?.invoke()
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(
                        modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
                        icon = UiRes.drawable.ui_drawable_code, title = stringResource(Res.string.adb_automation), showMore = true
                    )
                }
            }
            item { BottomSpace(paddingValues) }
        }
    })

}
