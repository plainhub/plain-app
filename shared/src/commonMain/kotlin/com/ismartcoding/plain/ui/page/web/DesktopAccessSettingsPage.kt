package com.ismartcoding.plain.ui.page.web

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.AccessFeatureType
import com.ismartcoding.plain.platform.openAppSettings
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.features.PermissionItem
import com.ismartcoding.plain.features.getWebList
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.isIgnoringBatteryOptimizations
import com.ismartcoding.plain.platform.openBatteryOptimizationSettings
import com.ismartcoding.plain.preferences.LocalApiPermissions
import com.ismartcoding.plain.preferences.LocalClipboardSync
import com.ismartcoding.plain.preferences.LocalKeepAwake
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.DesktopAccessSettingsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DesktopAccessSettingsPage(navController: NavHostController, webVM: DesktopAccessSettingsViewModel = viewModel { DesktopAccessSettingsViewModel() }) {
        WebSettingsProvider {
            val keepAwake = LocalKeepAwake.current
            val clipboardSync = LocalClipboardSync.current
        val scope = rememberCoroutineScope()
        val enabledPermissions = LocalApiPermissions.current
        val permissionList = remember { mutableStateOf(getWebList()) }
        val shouldIgnoreOptimize = remember { mutableStateOf(!isIgnoringBatteryOptimizations()) }
        val systemAlertWindow = remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.isGranted()) }
        val notificationListenerGranted = remember { mutableStateOf(Permission.NOTIFICATION_LISTENER.isGranted()) }
        val notificationsCard = AppFeatureType.NOTIFICATIONS.has()
        val listState = rememberLazyListState()
        val anchors = remember { mutableStateMapOf<AccessFeatureType, Rect>() }
        val switches = remember { mutableStateMapOf<AccessFeatureType, Rect>() }
        var listBounds by remember { mutableStateOf<Rect?>(null) }
        var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
        var overlaySize by remember { mutableStateOf(IntSize.Zero) }
        var bubbleFeature by remember { mutableStateOf<AccessFeatureType?>(null) }
        val density = LocalDensity.current

        LaunchedEffect(Unit) {
            AccessFeatureHighlight.pending.collect { f ->
                if (f == null) return@collect
                AccessFeatureHighlight.pending.value = null
                val index = itemIndexFor(f, permissionList.value, notificationsCard)
                if (index == null) return@collect
                delay(300)
                listState.animateScrollToItem(index)
                delay(150)
                with(density) {
                    val margin = 16.dp.roundToPx().toFloat()
                    val rect = anchors[f]
                    val viewport = listBounds
                    if (rect != null && viewport != null) {
                        when {
                            rect.bottom > viewport.bottom - margin -> listState.animateScrollBy(rect.bottom - viewport.bottom + margin)
                            rect.top < viewport.top + margin -> listState.animateScrollBy(rect.top - viewport.top - margin)
                        }
                    }
                }
                delay(150)
                bubbleFeature = f
            }
        }

        WebSettingsEffects(permissionList, shouldIgnoreOptimize, systemAlertWindow, notificationListenerGranted)

        PScaffold(topBar = {
            PTopAppBar(navController = navController, title = stringResource(Res.string.access_settings))
        }, content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        overlayOrigin = it.boundsInRoot().topLeft
                        overlaySize = it.size
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(top = paddingValues.calculateTopPadding())
                        .onGloballyPositioned { listBounds = it.boundsInRoot() }
                ) {
                    item {
                        TopSpace()
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(
                                modifier = Modifier.clickable { navController.navigate(Routing.Connections) },
                                icon = Res.drawable.devices, title = stringResource(Res.string.connections), showMore = true
                            )
                            PListItem(
                                modifier = Modifier.clickable { navController.navigate(Routing.WebSecurity) },
                                icon = Res.drawable.lock, title = stringResource(Res.string.security), showMore = true
                            )
                            PListItem(
                                modifier = Modifier.clickable { navController.navigate(Routing.HowToUse) },
                                icon = Res.drawable.info, title = stringResource(Res.string.how_to_use), showMore = true
                            )
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                    item { Subtitle(text = stringResource(Res.string.features)) }
                    itemsIndexed(permissionList.value) { index, m ->
                        val permission = m.permission
                        val af = permission.accessFeatureOf()
                        val baseModifier = if (af != null) {
                            Modifier.onGloballyPositioned { anchors[af] = it.boundsInRoot() }
                        } else {
                            Modifier
                        }
                        PListItem(
                            modifier = baseModifier
                                .then(PlainTheme.getCardModifier(index = index, size = permissionList.value.size))
                                .clickable { togglePermission(scope, m, !enabledPermissions.contains(permission.name)) },
                            icon = m.icon, title = permission.getText(),
                            subtitle = stringResource(if (m.granted) Res.string.system_permission_granted else Res.string.system_permission_not_granted)
                        ) {
                            if (af != null) {
                                Box(Modifier.onGloballyPositioned { switches[af] = it.boundsInRoot() }) {
                                    PSwitch(activated = enabledPermissions.contains(permission.name)) { enable ->
                                        togglePermission(scope, m, enable)
                                    }
                                }
                            } else {
                                PSwitch(activated = enabledPermissions.contains(permission.name)) { enable ->
                                    togglePermission(scope, m, enable)
                                }
                            }
                            HorizontalSpace(8.dp)
                        }
                    }
                    if (notificationsCard) {
                        item {
                            VerticalSpace(dp = 16.dp)
                            PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                                val m = PermissionItem.create(Res.drawable.bell, Permission.NOTIFICATION_LISTENER)
                                val permission = m.permission
                                val enabled = notificationListenerGranted.value && enabledPermissions.contains(permission.name)
                                PListItem(
                                    modifier = Modifier
                                        .onGloballyPositioned { anchors[AccessFeatureType.NOTIFICATIONS] = it.boundsInRoot() }
                                        .clickable { navController.navigate(Routing.NotificationSettings) },
                                    icon = m.icon, title = permission.getText(),
                                    subtitle = stringResource(if (notificationListenerGranted.value) Res.string.system_permission_granted else Res.string.system_permission_not_granted),
                                    separatedActions = true
                                ) {
                                    Box(Modifier.onGloballyPositioned { switches[AccessFeatureType.NOTIFICATIONS] = it.boundsInRoot() }) {
                                        PSwitch(activated = enabled) { enable -> togglePermission(scope, m, enable) }
                                    }
                                    HorizontalSpace(8.dp)
                                }
                            }
                        }
                    }
                    item {
                        VerticalSpace(dp = 16.dp)
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(
                                modifier = Modifier
                                    .onGloballyPositioned { anchors[AccessFeatureType.CLIPBOARD_SYNC] = it.boundsInRoot() }
                                    .clickable { navController.navigate(Routing.ClipboardHistory) },
                                icon = Res.drawable.content_paste, title = stringResource(Res.string.clipboard_sync),
                                separatedActions = true
                            ) {
                                Box(Modifier.onGloballyPositioned { switches[AccessFeatureType.CLIPBOARD_SYNC] = it.boundsInRoot() }) {
                                    PSwitch(activated = clipboardSync) { enable -> webVM.enableClipboardSync(enable) }
                                }
                                HorizontalSpace(8.dp)
                            }
                        }
                        if (clipboardSync) {
                            Tips(stringResource(Res.string.clipboard_sync_tips))
                        }
                    }
                    item {
                        VerticalSpace(dp = 16.dp)
                        val m = PermissionItem(null, Permission.NONE, setOf(Permission.NONE))
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(modifier = Modifier.clickable { openAppSettings() }, icon = m.icon, title = m.permission.getText(), showMore = true)
                        }
                    }
                    item {
                        VerticalSpace(dp = 16.dp); Subtitle(text = stringResource(Res.string.performance))
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(modifier = Modifier.clickable { webVM.enableKeepAwake(!keepAwake) }, title = stringResource(Res.string.keep_awake)) {
                                PSwitch(activated = keepAwake) { enable -> webVM.enableKeepAwake(enable) }
                                HorizontalSpace(8.dp)
                            }
                        }
                        Tips(stringResource(Res.string.keep_awake_tips))
                        VerticalSpace(dp = 16.dp)
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(modifier = Modifier.clickable {
                                    if (shouldIgnoreOptimize.value) webVM.requestIgnoreBatteryOptimization()
                                    else openBatteryOptimizationSettings()
                                }, title = stringResource(if (shouldIgnoreOptimize.value) Res.string.disable_battery_optimization else Res.string.battery_optimization_disabled), showMore = true)
                        }
                        Tips(stringResource(Res.string.battery_optimization_tips))
                    }
                    item {
                        VerticalSpace(dp = 16.dp)
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            PListItem(
                                modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
                                icon = Res.drawable.code, title = stringResource(Res.string.adb_automation), showMore = true
                            )
                        }
                    }
                    item { BottomSpace(paddingValues) }
                }
                bubbleFeature?.let { f ->
                    anchors[f]?.let { rect ->
                        AccessFeatureBubble(
                            anchor = rect,
                            switchAnchor = switches[f],
                            overlayOrigin = overlayOrigin,
                            overlaySize = overlaySize,
                            label = bubbleLabel(f),
                            onDismiss = { bubbleFeature = null },
                        )
                    }
                }
            }
        })
    }
}

private fun Permission.accessFeatureOf(): AccessFeatureType? = when (this) {
    Permission.WRITE_EXTERNAL_STORAGE -> AccessFeatureType.FILES
    Permission.WRITE_CONTACTS -> AccessFeatureType.CONTACTS
    Permission.READ_SMS -> AccessFeatureType.SMS
    Permission.WRITE_CALL_LOG -> AccessFeatureType.CALL_LOGS
    Permission.CALL_PHONE -> AccessFeatureType.CALL_PHONE
    Permission.READ_PHONE_NUMBERS -> AccessFeatureType.PHONE_NUMBER
    Permission.QUERY_ALL_PACKAGES -> AccessFeatureType.APPS
    Permission.NOTIFICATION_LISTENER -> AccessFeatureType.NOTIFICATIONS
    else -> null
}

/** LazyColumn item index of the row a feature maps to; null when the row does not exist. */
private fun itemIndexFor(feature: AccessFeatureType, rows: List<PermissionItem>, notificationsShown: Boolean): Int? {
    rows.forEachIndexed { i, m -> if (m.permission.accessFeatureOf() == feature) return 2 + i }
    var index = 2 + rows.size
    if (feature == AccessFeatureType.NOTIFICATIONS) return if (notificationsShown) index else null
    if (notificationsShown) index++
    if (feature == AccessFeatureType.CLIPBOARD_SYNC) return index
    return null
}

@Composable
private fun bubbleLabel(feature: AccessFeatureType): String = when (feature) {
    AccessFeatureType.FILES -> Permission.WRITE_EXTERNAL_STORAGE.getText()
    AccessFeatureType.CONTACTS -> Permission.WRITE_CONTACTS.getText()
    AccessFeatureType.SMS -> Permission.READ_SMS.getText()
    AccessFeatureType.CALL_LOGS -> Permission.WRITE_CALL_LOG.getText()
    AccessFeatureType.CALL_PHONE -> Permission.CALL_PHONE.getText()
    AccessFeatureType.PHONE_NUMBER -> Permission.READ_PHONE_NUMBERS.getText()
    AccessFeatureType.APPS -> Permission.QUERY_ALL_PACKAGES.getText()
    AccessFeatureType.NOTIFICATIONS -> Permission.NOTIFICATION_LISTENER.getText()
    AccessFeatureType.CLIPBOARD_SYNC -> stringResource(Res.string.clipboard_sync)
}
