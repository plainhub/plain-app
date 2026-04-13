package com.ismartcoding.plain.ui.page.web

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.isTV
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionItem
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.preferences.LocalApiPermissions
import com.ismartcoding.plain.preferences.LocalKeepAwake
import com.ismartcoding.plain.preferences.WebSettingsProvider
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.WebConsoleViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.PlainTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WebSettingsPage(navController: NavHostController, webVM: WebConsoleViewModel = viewModel()) {
    WebSettingsProvider {
        val context = LocalContext.current
        val keepAwake = LocalKeepAwake.current
        val scope = rememberCoroutineScope()
        val enabledPermissions = LocalApiPermissions.current
        val permissionList = remember { mutableStateOf(Permissions.getWebList(context)) }
        val shouldIgnoreOptimize = remember { mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) }
        val systemAlertWindow = remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.can(context)) }
        val notificationListenerGranted = remember { mutableStateOf(Permission.NOTIFICATION_LISTENER.can(context)) }

        WebSettingsEffects(permissionList, shouldIgnoreOptimize, systemAlertWindow, notificationListenerGranted)

        PScaffold(topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.web_settings))
        }, content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                    PCard {
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.Sessions) },
                            icon = R.drawable.devices, title = stringResource(R.string.sessions), showMore = true
                        )
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.WebSecurity) },
                            icon = R.drawable.lock, title = stringResource(R.string.security), showMore = true
                        )
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.HowToUse) },
                            icon = R.drawable.info, title = stringResource(R.string.how_to_use), showMore = true
                        )
                        PListItem(
                            modifier = Modifier.clickable { WebHelper.open(context, "https://plainapp.app/troubleshooting") },
                            icon = R.drawable.troubleshoot, title = stringResource(R.string.troubleshoot), showMore = true
                        )
                    }
                    VerticalSpace(dp = 16.dp)
                }
                item { Subtitle(text = stringResource(R.string.permissions)) }
                itemsIndexed(permissionList.value) { index, m ->
                    val permission = m.permission
                    PListItem(
                        modifier = PlainTheme
                            .getCardModifier(index = index, size = permissionList.value.size)
                            .clickable { togglePermission(scope, context, m, !enabledPermissions.contains(permission.name)) },
                        icon = m.icon, title = permission.getText(),
                        subtitle = stringResource(if (m.granted) R.string.system_permission_granted else R.string.system_permission_not_granted)
                    ) {
                        PSwitch(activated = enabledPermissions.contains(permission.name)) { enable -> togglePermission(scope, context, m, enable) }
                    }
                }
                if (AppFeatureType.NOTIFICATIONS.has()) {
                    item {
                        VerticalSpace(dp = 16.dp)
                        PCard {
                            val m = PermissionItem.create(context, R.drawable.bell, Permission.NOTIFICATION_LISTENER)
                            val permission = m.permission
                            PListItem(
                                modifier = Modifier.clickable { togglePermission(scope, context, m, !enabledPermissions.contains(permission.name)) },
                                icon = m.icon, title = permission.getText(),
                                subtitle = stringResource(if (notificationListenerGranted.value) R.string.system_permission_granted else R.string.system_permission_not_granted)
                            ) {
                                PSwitch(activated = enabledPermissions.contains(permission.name)) { enable -> togglePermission(scope, context, m, enable) }
                            }
                            if (enabledPermissions.contains(permission.name)) {
                                PListItem(
                                    modifier = Modifier.clickable { navController.navigate(Routing.NotificationSettings) },
                                    icon = R.drawable.settings, title = stringResource(R.string.notification_filter_settings),
                                    subtitle = stringResource(R.string.notification_filter_settings_desc), showMore = true
                                )
                            }
                        }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    val m = PermissionItem(null, Permission.NONE, setOf(Permission.NONE))
                    PCard {
                        PListItem(modifier = Modifier.clickable {
                            val intent = Intent(if (context.isTV()) Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.addCategory(Intent.CATEGORY_DEFAULT); intent.data = Uri.fromParts("package", context.packageName, null)
                            if (intent.resolveActivity(packageManager) != null) context.startActivity(intent) else DialogHelper.showMessage(R.string.not_supported_error)
                        }, icon = m.icon, title = m.permission.getText(), showMore = true)
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp); Subtitle(text = stringResource(R.string.performance))
                    PCard {
                        PListItem(modifier = Modifier.clickable { webVM.enableKeepAwake(context, !keepAwake) }, title = stringResource(R.string.keep_awake)) {
                            PSwitch(activated = keepAwake) { enable -> webVM.enableKeepAwake(context, enable) }
                        }
                    }
                    Tips(stringResource(R.string.keep_awake_tips))
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(modifier = Modifier.clickable {
                            if (shouldIgnoreOptimize.value) webVM.requestIgnoreBatteryOptimization()
                            else {
                                val intent = Intent(); intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS; context.startActivity(intent)
                            }
                        }, title = stringResource(if (shouldIgnoreOptimize.value) R.string.disable_battery_optimization else R.string.battery_optimization_disabled), showMore = true)
                    }
                    Tips(stringResource(R.string.battery_optimization_tips))
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(
                            modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
                            icon = R.drawable.code, title = stringResource(R.string.developer_options), showMore = true
                        )
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        })
    }
}
