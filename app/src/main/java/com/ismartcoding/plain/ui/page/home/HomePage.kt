package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.preferences.LocalWeb
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavHostController,
    mainVM: MainViewModel,
) {
    val webEnabled = LocalWeb.current
    val context = LocalContext.current
    var systemAlertWindow by remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.can(context)) }

    LaunchedEffect(Unit) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.can(context)
                }

                is WindowFocusChangedEvent -> {
                    mainVM.isVPNConnected = NetworkHelper.isVPNConnected(context)
                    mainVM.ip4s = NetworkHelper.getDeviceIP4s().filter { it.isNotEmpty() }
                    mainVM.ip4 = NetworkHelper.getDeviceIP4().ifEmpty { "127.0.0.1" }
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.can(context)
                }
            }
        }
    }

    PScaffold(
        topBar = { TopBarHome(navController) },
    ) { paddingValues ->
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
    ) {
        item {
            if (webEnabled) {
                if (mainVM.isVPNConnected) {
                    PAlert(
                        description = stringResource(id = R.string.vpn_web_conflict_warning),
                        AlertType.WARNING,
                    )
                }
                if (!systemAlertWindow) {
                    PAlert(
                        description = stringResource(id = R.string.system_alert_window_warning),
                        AlertType.WARNING,
                    ) {
                        PFilledButton(
                            text = stringResource(R.string.grant_permission),
                            small = true,
                            onClick = {
                                sendEvent(RequestPermissionsEvent(Permission.SYSTEM_ALERT_WINDOW))
                            },
                        )
                    }
                }
            }
        }
        item {
            HomeWeb(context, navController, mainVM, webEnabled)
            VerticalSpace(dp = 24.dp)
        }
        item {
            HomeShortcutGrid(navController = navController)
            VerticalSpace(dp = 16.dp)
        }
        item {
            BottomSpace(paddingValues)
        }
    }
    }
}
