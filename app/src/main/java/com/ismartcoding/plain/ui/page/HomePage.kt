package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.extensions.allowSensitivePermissions
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.LocalKeepScreenOn
import com.ismartcoding.plain.data.preference.LocalWeb
import com.ismartcoding.plain.features.StartHttpServerErrorEvent
import com.ismartcoding.plain.features.StopHttpServerDoneEvent
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.ActionButtonSettings
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.components.home.HomeItemEducation
import com.ismartcoding.plain.ui.components.home.HomeItemHardware
import com.ismartcoding.plain.ui.components.home.HomeItemSocial
import com.ismartcoding.plain.ui.components.home.HomeItemStorage
import com.ismartcoding.plain.ui.components.home.HomeItemTools
import com.ismartcoding.plain.ui.components.home.HomeItemWork
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.theme.green
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    val webConsole = LocalWeb.current
    val keepScreenOn = LocalKeepScreenOn.current
    val configuration = LocalConfiguration.current
    val itemWidth = (configuration.screenWidthDp.dp - 40.dp) / 4
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    LaunchedEffect(Unit) {
        events.add(
            receiveEventHandler<StartHttpServerErrorEvent> {
                viewModel.httpServerError.value = HttpServerManager.getErrorMessage()
            }
        )

        events.add(
            receiveEventHandler<StopHttpServerDoneEvent> {
                viewModel.httpServerError.value = HttpServerManager.getErrorMessage()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            events.forEach { it.cancel() }
        }
    }

    if (webConsole) {
        viewModel.startTimer()
    }

    PScaffold(
        navController,
        navigationIcon = {
            ActionButtonSettings {
                navController.navigate(RouteName.SETTINGS)
            }
        },
        actions = {
            PIconButton(
                imageVector = Icons.Outlined.Computer,
                contentDescription = stringResource(R.string.web_console),
                tint = MaterialTheme.colorScheme.onSurface,
                showBadge = viewModel.showWebBadge.value,
                badgeColor = if (viewModel.httpServerError.value.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.green(),
            ) {
                navController.navigate(RouteName.WEB_CONSOLE)
            }
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
                    // ScanDialog().show()
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
                    Icons.Outlined.Chat,
                    stringResource(R.string.my_phone),
                )
            }
        },
    ) {
        LazyColumn {
            item {
                HomeItemStorage(navController, itemWidth)
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                HomeItemWork(itemWidth)
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (context.allowSensitivePermissions()) {
                item {
                    HomeItemSocial(navController, itemWidth)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            if (BuildConfig.DEBUG) {
                item {
                    HomeItemEducation(navController, itemWidth)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            item {
                HomeItemTools(navController, itemWidth)
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (BuildConfig.DEBUG) {
                item {
                    HomeItemHardware(navController, itemWidth, viewModel)
                }
            }
            item {
                BottomSpace()
            }
        }
    }
}
