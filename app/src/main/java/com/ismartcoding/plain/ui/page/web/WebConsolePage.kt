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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isTV
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.*
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionResultEvent
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.RequestPermissionEvent
import com.ismartcoding.plain.features.StartHttpServerErrorEvent
import com.ismartcoding.plain.features.StopHttpServerDoneEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.models.WebConsoleViewModel
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.canvas
import com.ismartcoding.plain.ui.theme.cardBack
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebConsolePage(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    viewModel: WebConsoleViewModel = viewModel(),
) {
    WebSettingsProvider {
        val context = LocalContext.current
        val webConsole = LocalWeb.current
        val scope = rememberCoroutineScope()
        var isHttps by remember { mutableStateOf(true) }
        val httpsPort = LocalHttpsPort.current
        val httpPort = LocalHttpPort.current
        val passwordType = LocalPasswordType.current
        val password = LocalPassword.current
        var isMenuOpen by remember { mutableStateOf(false) }
        val enabledPermissions = LocalApiPermissions.current
        var permissionList by remember { mutableStateOf(Permissions.getWebList(context)) }
        var portDialogVisible by remember { mutableStateOf(false) }
        val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

        LaunchedEffect(Unit) {
            events.add(
                receiveEventHandler<PermissionResultEvent> {
                    permissionList = Permissions.getWebList(context)
                }
            )

            events.add(
                receiveEventHandler<StartHttpServerErrorEvent> {
                    mainViewModel.httpServerError.value = HttpServerManager.getErrorMessage()
                }
            )

            events.add(
                receiveEventHandler<StopHttpServerDoneEvent> {
                    mainViewModel.httpServerError.value = HttpServerManager.getErrorMessage()
                }
            )
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
                    viewModel.dig(context, httpPort = httpPort)
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
                                    text = stringResource(R.string.fix),
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            if (HttpServerManager.portsInUse.contains(TempData.httpPort)) {
                                                HttpPortPreference.putAsync(context, HttpServerManager.httpPorts.filter { it != TempData.httpPort }.random())
                                            }
                                            if (HttpServerManager.portsInUse.contains(TempData.httpsPort)) {
                                                HttpsPortPreference.putAsync(context, HttpServerManager.httpsPorts.filter { it != TempData.httpsPort }.random())
                                            }
                                            coMain {
                                                DialogHelper.showConfirmDialog(
                                                    context,
                                                    context.getString(R.string.restart_app_title),
                                                    context.getString(R.string.restart_app_message),
                                                ) {
                                                    AppHelper.relaunch(context)
                                                }
                                            }
                                        }
                                    },
                                )
                            } else {
                                MiniOutlineButton(
                                    text = stringResource(R.string.relaunch_app),
                                    onClick = {
                                        AppHelper.relaunch(context)
                                    },
                                )
                            }
                        }
                    } else if (NetworkHelper.isVPNConnected(context)) {
                        Alert(title = stringResource(id = R.string.warning), description = stringResource(id = R.string.vpn_web_conflict_warning), AlertType.WARNING)
                    }
                    DisplayText(
                        description = stringResource(id = R.string.web_console_desc),
                    )
                }
                item {
                    PListItem(
                        title = stringResource(R.string.enable),
                    ) {
                        PSwitch(
                            activated = webConsole,
                        ) {
                            viewModel.enableWebConsole(context, !webConsole)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    BlockRadioButton(
                        selected = if (isHttps) 0 else 1,
                        onSelected = { isHttps = it == 0 },
                        itemRadioGroups =
                        listOf(
                            BlockRadioGroupButtonItem(
                                text = stringResource(R.string.recommended_https),
                                onClick = {},
                            ) {},
                            BlockRadioGroupButtonItem(
                                text = "HTTP",
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
                        title = stringResource(R.string.password),
                        desc = PasswordType.getText(passwordType),
                        value = if (passwordType != PasswordType.NONE.value) password else "",
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
                        PListItem(
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
                            title = permission.getText(),
                            desc =
                            stringResource(
                                if (m.granted) R.string.system_permission_granted else R.string.system_permission_not_granted,
                            ),
                            showMore = permission == Permission.SYSTEM_ALERT_WINDOW,
                            onClick = {
                                scope.launch {
                                    val enable = withIO { !permission.isEnabledAsync(context) }
                                    withIO { ApiPermissionsPreference.putAsync(context, permission, enable) }
                                    if (permission == Permission.SYSTEM_ALERT_WINDOW) {
                                        sendEvent(RequestPermissionEvent(Permission.SYSTEM_ALERT_WINDOW))
                                    } else {
                                        if (enable) {
                                            if (m.granted) {
                                                return@launch
                                            }
                                            sendEvent(RequestPermissionEvent(permission))
                                        }
                                    }
                                }
                            },
                        ) {
                            if (permission != Permission.SYSTEM_ALERT_WINDOW) {
                                PSwitch(activated = enabledPermissions.contains(permission.name)) { enable ->
                                    scope.launch {
                                        withIO {
                                            ApiPermissionsPreference.putAsync(context, permission, enable)
                                        }
                                        if (enable) {
                                            if (m.granted) {
                                                return@launch
                                            }
                                            sendEvent(RequestPermissionEvent(permission))
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
                    DialogHelper.showConfirmDialog(
                        context,
                        context.getString(R.string.restart_app_title),
                        context.getString(R.string.restart_app_message),
                    ) {
                        AppHelper.relaunch(context)
                    }
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
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
