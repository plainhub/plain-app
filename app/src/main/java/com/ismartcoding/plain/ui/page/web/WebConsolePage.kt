package com.ismartcoding.plain.ui.page.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.*
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.WebConsoleViewModel
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.palette.onDark
import com.ismartcoding.plain.ui.theme.palette.onLight
import com.ismartcoding.plain.web.HttpServerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebConsolePage(
    navController: NavHostController,
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

        val launcherMap = mutableMapOf<Permission, ManagedActivityResultLauncher<String, Boolean>>()
        val intentLauncherMap = mutableMapOf<Permission, ActivityResultLauncher<Intent>>()

        setOf(
            Permission.CAMERA,
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.CALL_PHONE,
            Permission.WRITE_SETTINGS,
            Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG,
            Permission.READ_CONTACTS, Permission.WRITE_CONTACTS,
            Permission.READ_SMS, Permission.SEND_SMS,
        ).forEach { permission ->
            launcherMap[permission] = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                permissionList = Permissions.getWebList(context)
            }
        }

        setOf(
            Permission.WRITE_SETTINGS, Permission.WRITE_EXTERNAL_STORAGE, Permission.SYSTEM_ALERT_WINDOW
        ).forEach { permission ->
            intentLauncherMap[permission] = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                permissionList = Permissions.getWebList(context)
            }
        }

        PScaffold(navController, actions = {
            MiniOutlineButton(
                text = stringResource(R.string.sessions),
                onClick = {
                    navController.navigate(RouteName.SESSIONS)
                },
            )
            ActionButtonMore {
                isMenuOpen = !isMenuOpen
            }
            DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }, content = {
                DropdownMenuItem(onClick = {
                    isMenuOpen = false
                    val title = context.getString(R.string.https_certificate_signature)
                    val content = HttpServerManager.getSSLSignature(context).joinToString(" ") { "%02x".format(it).uppercase() }
                    navController.navigate("${RouteName.TEXT.name}?title=${title}&content=${content}")
                }, text = {
                    Text(text = stringResource(R.string.https_certificate_signature))
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
                    val errorMessage = if (MainApp.instance.httpServerError.isNotEmpty()) {
                        MainApp.instance.httpServerError
                    } else if (webConsole && MainApp.instance.httpServer == null) {
                        stringResource(id = R.string.http_server_failed)
                    } else {
                        ""
                    }
                    if (errorMessage.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface onDark MaterialTheme.colorScheme.inverseOnSurface, shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(16.dp),
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    DisplayText(
                        text = stringResource(R.string.web_console), desc = stringResource(id = R.string.web_console_desc)
                    )
                }
                item {
                    PListItem(
                        title = stringResource(R.string.enable),
                    ) {
                        PSwitch(
                            activated = webConsole
                        ) {
                            viewModel.enableWebConsole(context, scope, !webConsole)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    BlockRadioButton(
                        selected = if (isHttps) 0 else 1,
                        onSelected = { isHttps = it == 0 },
                        itemRadioGroups = listOf(
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
                    BrowserPreview(isHttps, httpPort, httpsPort, onEditPort = {
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
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.permissions),
                    )
                }
                item {
                    permissionList.forEach { m ->
                        val permission = m.permission
                        if (permission == Permission.NONE) {
                            PListItem(
                                title = permission.getText(),
                                showMore = true,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.fromParts("package", context.packageName, null)
                                    context.startActivity(intent)
                                })
                        } else {
                            PListItem(
                                title = permission.getText(),
                                desc = stringResource(if (m.granted) R.string.system_permission_granted else R.string.system_permission_not_granted),
                                showMore = permission == Permission.SYSTEM_ALERT_WINDOW,
                                onClick = {
                                    val enable = !permission.isEnabled(context)
                                    ApiPermissionsPreference.put(context, scope, permission, enable)
                                    if (permission == Permission.SYSTEM_ALERT_WINDOW) {
                                        intentLauncherMap[permission]?.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                    } else {
                                        if (enable) {
                                            if (m.granted) {
                                                return@PListItem
                                            }
                                            permission.request(context, launcher = launcherMap[permission], intentLauncher = intentLauncherMap[permission])
                                        }
                                    }
                                }) {
                                if (permission != Permission.SYSTEM_ALERT_WINDOW) {
                                    PSwitch(activated = enabledPermissions.contains(permission.name)) { enable ->
                                        ApiPermissionsPreference.put(context, scope, permission, enable)
                                        if (enable) {
                                            if (m.granted) {
                                                return@PSwitch
                                            }
                                            permission.request(context, launcher = launcherMap[permission], intentLauncher = intentLauncherMap[permission])
                                        }
                                    }
                                }
                            }
                        }
                    }
                    BottomSpace()
                }
            }
        })

        RadioDialog(visible = portDialogVisible,
            title = stringResource(if (isHttps) R.string.https_port else R.string.http_port),
            options = (if (isHttps) listOf(8043, 8143, 8243, 8343, 8443, 8543, 8643, 8743, 8843, 8943) else listOf(8080, 8180, 8280, 8380, 8480, 8580, 8680, 8780, 8880, 8980)).map {
                RadioDialogOption(
                    text = it.toString(),
                    selected = if (isHttps) it == httpsPort else it == httpPort,
                ) {
                    if (isHttps) {
                        HttpsPortPreference.put(context, scope, it)
                    } else {
                        HttpPortPreference.put(context, scope, it)
                    }
                    DialogHelper.showConfirmDialog(context, context.getString(R.string.restart_app_title), context.getString(R.string.restart_app_message)) {
                        triggerRebirth(context)
                    }
                }
            }) {
            portDialogVisible = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserPreview(isHttps: Boolean, httpPort: Int, httpsPort: Int, onEditPort: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .background(
                color = MaterialTheme.colorScheme.surface onDark MaterialTheme.colorScheme.inverseOnSurface, shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface, shape = RoundedCornerShape(8.dp)
                    ), verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer {
                    Text(
                        text = "${if (isHttps) "https" else "http"}://${NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" }}:${if (isHttps) httpsPort else httpPort}",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                        textAlign = TextAlign.Start,
                    )
                }
                PIconButton(imageVector = Icons.Rounded.Edit,
                    modifier = Modifier
                        .height(16.dp)
                        .width(16.dp),
                    contentDescription = stringResource(id = R.string.edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onEditPort()
                    })
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            text = stringResource(id = R.string.enter_this_address_tips),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun triggerRebirth(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent!!.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}