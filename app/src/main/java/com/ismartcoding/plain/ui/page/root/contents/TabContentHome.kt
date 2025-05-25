package com.ismartcoding.plain.ui.page.root.contents

import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.RequestPermissionsEvent
import com.ismartcoding.plain.features.WindowFocusChangedEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preference.HttpPortPreference
import com.ismartcoding.plain.preference.HttpsPortPreference
import com.ismartcoding.plain.preference.LocalWeb
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.NetworkErrorBanner
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.page.root.components.HomeFeatures
import com.ismartcoding.plain.ui.page.root.components.HomeWeb
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabContentHome(
    navController: NavHostController,
    mainVM: MainViewModel,
    paddingValues: PaddingValues
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val itemWidth = (configuration.screenWidthDp.dp - 34.dp) / 3f
    val webEnabled = LocalWeb.current
    val context = LocalContext.current
    var systemAlertWindow by remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.can(context)) }
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.can(context)
                }

                is WindowFocusChangedEvent -> {
                    mainVM.isVPNConnected = NetworkHelper.isVPNConnected(context)
                    mainVM.isNetworkConnected = NetworkHelper.isNetworkConnected(context)
                }
            }
        }
    }

    LazyColumn(Modifier
        .fillMaxSize()
        .padding(top = paddingValues.calculateTopPadding())) {
        item {
            TopSpace()
        }
        item {
            NetworkErrorBanner(
                isVisible = !mainVM.isNetworkConnected && webEnabled
            )
        }
        item {
            if (webEnabled) {
                if (mainVM.httpServerError.isNotEmpty()) {
                    PAlert(title = stringResource(id = R.string.error), description = mainVM.httpServerError, AlertType.ERROR) {
                        if (HttpServerManager.portsInUse.isNotEmpty()) {
                            PMiniOutlineButton(
                                label = stringResource(R.string.change_port),
                                click = {
                                    scope.launch(Dispatchers.IO) {
                                        if (HttpServerManager.portsInUse.contains(TempData.httpPort)) {
                                            HttpPortPreference.putAsync(context, HttpServerManager.httpPorts.filter { it != TempData.httpPort }.random())
                                        }
                                        if (HttpServerManager.portsInUse.contains(TempData.httpsPort)) {
                                            HttpsPortPreference.putAsync(context, HttpServerManager.httpsPorts.filter { it != TempData.httpsPort }.random())
                                        }
                                        coMain {
                                            AlertDialog.Builder(context)
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
                        PMiniOutlineButton(
                            label = stringResource(R.string.relaunch_app),
                            modifier = Modifier.padding(start = 16.dp),
                            click = {
                                AppHelper.relaunch(context)
                            },
                        )
                    }
                } else {
                    if (mainVM.isVPNConnected) {
                        PAlert(title = stringResource(id = R.string.attention), description = stringResource(id = R.string.vpn_web_conflict_warning), AlertType.WARNING)
                    }
                    if (!systemAlertWindow) {
                        PAlert(title = stringResource(id = R.string.attention), description = stringResource(id = R.string.system_alert_window_warning), AlertType.WARNING) {
                            PMiniOutlineButton(
                                label = stringResource(R.string.grant_permission),
                                click = {
                                    sendEvent(RequestPermissionsEvent(Permission.SYSTEM_ALERT_WINDOW))
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            HomeWeb(context, navController, mainVM, webEnabled)
            VerticalSpace(dp = 16.dp)
        }
        item {
            HomeFeatures(navController, itemWidth)
        }
        item {
            BottomSpace(paddingValues)
        }
    }
}