package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.preference.HttpPortPreference
import com.ismartcoding.plain.data.preference.HttpsPortPreference
import com.ismartcoding.plain.data.preference.LocalKeepScreenOn
import com.ismartcoding.plain.data.preference.LocalWeb
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.RequestPermissionsEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.ActionButtonSettings
import com.ismartcoding.plain.ui.base.Alert
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.MiniOutlineButton
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.rememberLifecycleEvent
import com.ismartcoding.plain.ui.components.home.HomeFeatures
import com.ismartcoding.plain.ui.components.home.HomeWeb
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavHostController,
    viewModel: MainViewModel,
) {
    WebSettingsProvider {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var isMenuOpen by remember { mutableStateOf(false) }
        val keepScreenOn = LocalKeepScreenOn.current
        val configuration = LocalConfiguration.current
        val itemWidth = (configuration.screenWidthDp.dp - 96.dp) / 3
        val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }
        val webEnabled = LocalWeb.current
        var systemAlertWindow by remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.can(context)) }
        var isVPNConnected by remember { mutableStateOf(NetworkHelper.isVPNConnected(context)) }

        val lifecycleEvent = rememberLifecycleEvent()
        LaunchedEffect(lifecycleEvent) {
            if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
                isVPNConnected = NetworkHelper.isVPNConnected(context)
            }
        }

        LaunchedEffect(Unit) {
            events.add(
                receiveEventHandler<PermissionsResultEvent> {
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.can(context)
                }
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                events.forEach { it.cancel() }
            }
        }

        PScaffold(
            navController,
            navigationIcon = {
                ActionButtonSettings {
                    navController.navigate(RouteName.SETTINGS)
                }
            },
            actions = {
                ActionButtonMore {
                    isMenuOpen = !isMenuOpen
                }
                PDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }, content = {
                    DropdownMenuItem(onClick = {
                        isMenuOpen = false
                        scope.launch(Dispatchers.IO) {
                            ScreenHelper.keepScreenOnAsync(context, !keepScreenOn)
                        }
                    }, text = {
                        Row {
                            Text(
                                text = stringResource(R.string.keep_screen_on),
                                modifier = Modifier.padding(top = 14.dp),
                            )
                            Checkbox(checked = keepScreenOn, onCheckedChange = {
                                isMenuOpen = false
                                scope.launch(Dispatchers.IO) {
                                    ScreenHelper.keepScreenOnAsync(context, it)
                                }
                            })
                        }
                    })
                    DropdownMenuItem(onClick = {
                        isMenuOpen = false
                        navController.navigate(RouteName.SCAN)
                    }, text = {
                        Text(text = stringResource(R.string.scan_qrcode))
                    })
                })
            },
            floatingActionButton = {
                FloatingActionButton(
                    modifier =
                    Modifier
                        .padding(bottom = 32.dp),
                    onClick = {
                        navController.navigate(RouteName.CHAT)
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Chat,
                        stringResource(R.string.file_transfer_assistant),
                    )
                }
            },
        ) {
            LazyColumn {
                item {
                    TopSpace()
                    if (webEnabled) {
                        if (viewModel.httpServerError.value.isNotEmpty()) {
                            Alert(title = stringResource(id = R.string.error), description = viewModel.httpServerError.value, AlertType.ERROR) {
                                if (HttpServerManager.portsInUse.isNotEmpty()) {
                                    MiniOutlineButton(
                                        text = stringResource(R.string.change_port),
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                if (HttpServerManager.portsInUse.contains(TempData.httpPort)) {
                                                    HttpPortPreference.putAsync(context, HttpServerManager.httpPorts.filter { it != TempData.httpPort }.random())
                                                }
                                                if (HttpServerManager.portsInUse.contains(TempData.httpsPort)) {
                                                    HttpsPortPreference.putAsync(context, HttpServerManager.httpsPorts.filter { it != TempData.httpsPort }.random())
                                                }
                                                coMain {
                                                    MaterialAlertDialogBuilder(context)
                                                        .setTitle(R.string.restart_app_title)
                                                        .setMessage(R.string.restart_app_message)
                                                        .setPositiveButton(R.string.relaunch_app) { _, _ ->
                                                            AppHelper.relaunch(context)
                                                        }
                                                        .setCancelable(false)
                                                        .create()
                                                        .show()
                                                }
                                            }
                                        },
                                    )
                                }
                                MiniOutlineButton(
                                    text = stringResource(R.string.relaunch_app),
                                    modifier = Modifier.padding(start = 16.dp),
                                    onClick = {
                                        AppHelper.relaunch(context)
                                    },
                                )
                            }
                        } else {
                            if (isVPNConnected) {
                                Alert(title = stringResource(id = R.string.warning), description = stringResource(id = R.string.vpn_web_conflict_warning), AlertType.WARNING)
                            }
                            if (!systemAlertWindow) {
                                Alert(title = stringResource(id = R.string.warning), description = stringResource(id = R.string.system_alert_window_warning), AlertType.WARNING) {
                                    MiniOutlineButton(
                                        text = stringResource(R.string.grant_permission),
                                        onClick = {
                                            sendEvent(RequestPermissionsEvent(Permission.SYSTEM_ALERT_WINDOW))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    HomeWeb(context, navController, viewModel, webEnabled)
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    HomeFeatures(navController, itemWidth)
                }
                item {
                    BottomSpace()
                }
            }
        }
    }
}
