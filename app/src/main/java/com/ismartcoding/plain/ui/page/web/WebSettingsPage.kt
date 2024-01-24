package com.ismartcoding.plain.ui.page.web

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isTV
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.ApiPermissionsPreference
import com.ismartcoding.plain.data.preference.HttpPortPreference
import com.ismartcoding.plain.data.preference.HttpsPortPreference
import com.ismartcoding.plain.data.preference.LocalApiPermissions
import com.ismartcoding.plain.data.preference.LocalHttpPort
import com.ismartcoding.plain.data.preference.LocalHttpsPort
import com.ismartcoding.plain.data.preference.LocalPassword
import com.ismartcoding.plain.data.preference.LocalPasswordType
import com.ismartcoding.plain.data.preference.LocalWeb
import com.ismartcoding.plain.data.preference.WebSettingsProvider
import com.ismartcoding.plain.features.IgnoreBatteryOptimizationResultEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionResultEvent
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.RequestPermissionEvent
import com.ismartcoding.plain.features.RequestPermissionsEvent
import com.ismartcoding.plain.features.StartHttpServerStateEvent
import com.ismartcoding.plain.features.StopHttpServerDoneEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.Alert
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BlockRadioButton
import com.ismartcoding.plain.ui.base.BlockRadioGroupButtonItem
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.DisplayText
import com.ismartcoding.plain.ui.base.MiniOutlineButton
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PMainSwitch
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.rememberLifecycleEvent
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.WebConsoleViewModel
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.canvas
import com.ismartcoding.plain.ui.theme.cardBack
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSettingsPage(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    viewModel: WebConsoleViewModel = viewModel(),
) {
    WebSettingsProvider {
        val context = LocalContext.current
        val webConsole = LocalWeb.current
        val scope = rememberCoroutineScope()
        var isHttps by remember { mutableStateOf(false) }
        val httpsPort = LocalHttpsPort.current
        val httpPort = LocalHttpPort.current
        val passwordType = LocalPasswordType.current
        val password = LocalPassword.current
        var isMenuOpen by remember { mutableStateOf(false) }
        val enabledPermissions = LocalApiPermissions.current
        var permissionList by remember { mutableStateOf(Permissions.getWebList(context)) }
        var portDialogVisible by remember { mutableStateOf(false) }
        var showIgnoreOptimizeWarning by remember { mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) }
        var systemAlertWindow by remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.can(context)) }
        var isVPNConnected by remember { mutableStateOf(NetworkHelper.isVPNConnected(context)) }
        val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

        val lifecycleEvent = rememberLifecycleEvent()
        LaunchedEffect(lifecycleEvent) {
            if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
                showIgnoreOptimizeWarning = !powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
                isVPNConnected = NetworkHelper.isVPNConnected(context)
            }
        }

        LaunchedEffect(Unit) {
            events.add(
                receiveEventHandler<PermissionResultEvent> {
                    permissionList = Permissions.getWebList(context)
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.can(context)
                }
            )

            events.add(
                receiveEventHandler<PermissionsResultEvent> {
                    permissionList = Permissions.getWebList(context)
                }
            )

            events.add(
                receiveEventHandler<StartHttpServerStateEvent> {
                    mainViewModel.httpServerError.value = HttpServerManager.getErrorMessage()
                }
            )

            events.add(
                receiveEventHandler<StopHttpServerDoneEvent> {
                    mainViewModel.httpServerError.value = HttpServerManager.getErrorMessage()
                }
            )

            events.add(receiveEventHandler<IgnoreBatteryOptimizationResultEvent> {
                coIO {
                    DialogHelper.showLoading()
                    delay(1000) // MIUI 12 test 1 second to get the final correct result.
                    DialogHelper.hideLoading()
                    showIgnoreOptimizeWarning = !powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
                }
            })
        }

        DisposableEffect(Unit) {
            onDispose {
                events.forEach { it.cancel() }
            }
        }

        PScaffold(navController, topBarTitle = stringResource(id = R.string.web_console), actions = {
            MiniOutlineButton(
                text = stringResource(R.string.sessions),
                onClick = {
                    navController.navigate(RouteName.SESSIONS)
                },
            )
            ActionButtonMore {
                isMenuOpen = !isMenuOpen
            }
            PDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }, content = {
                DropdownMenuItem(onClick = {
                    isMenuOpen = false
                    navController.navigate(RouteName.WEB_SECURITY)
                }, text = {
                    Text(text = stringResource(R.string.security))
                })
                DropdownMenuItem(onClick = {
                    isMenuOpen = false
                    viewModel.dig(context)
                }, text = {
                    Text(text = stringResource(R.string.http_server_diagnostics))
                })
                DropdownMenuItem(onClick = {
                    isMenuOpen = false
                    navController.navigate(RouteName.WEB_DEV)
                }, text = {
                    Text(text = stringResource(R.string.testing_token))
                })
            })
        }, content = {
            LazyColumn {
                item {
                    if (mainViewModel.httpServerError.value.isNotEmpty()) {
                        Alert(title = stringResource(id = R.string.error), description = mainViewModel.httpServerError.value, AlertType.ERROR) {
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
                        if (NetworkHelper.isVPNConnected(context)) {
                            Alert(title = stringResource(id = R.string.warning), description = stringResource(id = R.string.vpn_web_conflict_warning), AlertType.WARNING)
                        }
                        if (!systemAlertWindow) {
                            Alert(title = stringResource(id = R.string.warning), description = stringResource(id = R.string.system_alert_window_warning), AlertType.WARNING) {
                                MiniOutlineButton(
                                    text = stringResource(R.string.grant_permission),
                                    onClick = {
                                        sendEvent(RequestPermissionEvent(Permission.SYSTEM_ALERT_WINDOW))
                                    },
                                )
                            }
                        }
//                        if (showIgnoreOptimizeWarning) {
//                            Alert(title = stringResource(id = R.string.warning), description = stringResource(id = R.string.optimized_batter_usage_warning), AlertType.WARNING) {
//                                MiniOutlineButton(
//                                    text = stringResource(R.string.fix),
//                                    onClick = {
//                                        viewModel.requestIgnoreBatteryOptimization()
//                                    },
//                                )
//                            }
//                        }
                    }
                    DisplayText(
                        description = stringResource(id = R.string.web_console_desc),
                    )
                }
                item {
                    PMainSwitch(
                        title = stringResource(id = R.string.allow_remote_access_from_pc),
                        activated = webConsole,
                    ) {
                        viewModel.enableWebConsole(context, it)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    BlockRadioButton(
                        selected = if (isHttps) 1 else 0,
                        onSelected = { isHttps = it == 1 },
                        itemRadioGroups =
                        listOf(
                            BlockRadioGroupButtonItem(
                                text = "HTTP",
                                onClick = {},
                            ) {},
                            BlockRadioGroupButtonItem(
                                text = stringResource(R.string.advanced_https),
                                onClick = {},
                            ) {},
                        ),
                    )
                    BrowserPreview(context, isHttps, httpPort, httpsPort, onEditPort = {
                        portDialogVisible = true
                    })
                    if (isHttps) {
                        Tips(text = stringResource(id = R.string.browser_https_error_tips))
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                item {
                    PListItem(
                        title = if (passwordType == PasswordType.NONE.value) stringResource(id = R.string.password_type_none) else stringResource(R.string.password),
                        value = if (passwordType == PasswordType.NONE.value) "" else password,
                        onClick = {
                            navController.navigate(RouteName.PASSWORD)
                        },
                        showMore = true,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Subtitle(
                        text = stringResource(R.string.permissions),
                    )
                }
                items(permissionList) { m ->
                    val permission = m.permission
                    if (permission == Permission.NONE) {
                        VerticalSpace(dp = 16.dp)
                        PListItem(
                            icon = m.icon,
                            title = permission.getText(),
                            showMore = true,
                            onClick = {
                                val intent =
                                    Intent(
                                        if (context.isTV()) Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    )
                                intent.addCategory(Intent.CATEGORY_DEFAULT)
                                intent.data = Uri.fromParts("package", context.packageName, null)
                                if (intent.resolveActivity(packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    DialogHelper.showMessage(R.string.not_supported_error)
                                }
                            },
                        )
                    } else {
                        PListItem(
                            icon = m.icon,
                            title = permission.getText(),
                            desc =
                            stringResource(
                                if (m.granted) R.string.system_permission_granted else R.string.system_permission_not_granted,
                            ),
                            onClick = {
                                scope.launch {
                                    val enable = withIO { !permission.isEnabledAsync(context) }
                                    withIO { ApiPermissionsPreference.putAsync(context, permission, enable) }
                                    if (enable) {
                                        val ps = m.permissions.filter { !it.can(context) }
                                        if (ps.isNotEmpty()) {
                                            if (ps.size == 1) {
                                                sendEvent(RequestPermissionEvent(ps[0]))
                                            } else {
                                                sendEvent(RequestPermissionsEvent(ps.toSet()))
                                            }
                                        }
                                    }
                                }
                            },
                        ) {
                            if (permission != Permission.SYSTEM_ALERT_WINDOW) {
                                PSwitch(activated = enabledPermissions.contains(permission.name)) { enable ->
                                    scope.launch {
                                        withIO { ApiPermissionsPreference.putAsync(context, permission, enable) }
                                        if (enable) {
                                            val ps = m.permissions.filter { !it.can(context) }
                                            if (ps.isNotEmpty()) {
                                                if (ps.size == 1) {
                                                    sendEvent(RequestPermissionEvent(ps[0]))
                                                } else {
                                                    sendEvent(RequestPermissionsEvent(ps.toSet()))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    BottomSpace()
                }
            }
        })

        RadioDialog(
            visible = portDialogVisible,
            title = stringResource(if (isHttps) R.string.https_port else R.string.http_port),
            options =
            (if (isHttps) HttpServerManager.httpsPorts else HttpServerManager.httpPorts).map {
                RadioDialogOption(
                    text = it.toString(),
                    selected = if (isHttps) it == httpsPort else it == httpPort,
                ) {
                    scope.launch(Dispatchers.IO) {
                        if (isHttps) {
                            HttpsPortPreference.putAsync(context, it)
                        } else {
                            HttpPortPreference.putAsync(context, it)
                        }
                    }
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
            },
        ) {
            portDialogVisible = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserPreview(
    context: Context,
    isHttps: Boolean,
    httpPort: Int,
    httpsPort: Int,
    onEditPort: () -> Unit,
) {
    val ip4 = remember { NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" } }
    val ip4s = remember { NetworkHelper.getDeviceIP4s().filter { it != ip4 } }
    val showContextMenu = remember { mutableStateOf(false) }
    val defaultUrl = "${if (isHttps) "https" else "http"}://$ip4:${if (isHttps) httpsPort else httpPort}"
    Column(
        modifier =
        Modifier
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.cardBack(),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.canvas(),
                    shape = RoundedCornerShape(8.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionContainer {
                ClickableText(
                    text = AnnotatedString(defaultUrl),
                    modifier = Modifier.padding(start = 16.dp),
                    style =
                    TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                    ),
                    onClick = {
                        val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), defaultUrl)
                        clipboardManager.setPrimaryClip(clip)
                        DialogHelper.showConfirmDialog(context, "", context.getString(R.string.copied_to_clipboard_format, defaultUrl))
                    },
                )
            }
            PIconButton(
                imageVector = Icons.Rounded.Edit,
                modifier =
                Modifier
                    .height(16.dp)
                    .width(16.dp),
                contentDescription = stringResource(id = R.string.edit),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    onEditPort()
                },
            )
            if (ip4s.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier =
                    Modifier
                        .wrapContentSize(Alignment.TopEnd),
                ) {
                    PIconButton(
                        imageVector = Icons.Rounded.MoreVert,
                        modifier =
                        Modifier
                            .height(16.dp)
                            .width(16.dp),
                        contentDescription = stringResource(id = R.string.more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            showContextMenu.value = true
                        },
                    )
                    PDropdownMenu(
                        expanded = showContextMenu.value,
                        onDismissRequest = { showContextMenu.value = false },
                    ) {
                        ip4s.forEach { ip ->
                            val url = "${if (isHttps) "https" else "http"}://$ip:${if (isHttps) httpsPort else httpPort}"
                            DropdownMenuItem(text = { Text(url) }, onClick = {
                                showContextMenu.value = false
                                val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), url)
                                clipboardManager.setPrimaryClip(clip)
                                DialogHelper.showConfirmDialog(context, "", context.getString(R.string.copied_to_clipboard_format, url))
                            })
                        }
                    }
                }
            }
        }
        Text(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = stringResource(id = R.string.enter_this_address_tips),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
