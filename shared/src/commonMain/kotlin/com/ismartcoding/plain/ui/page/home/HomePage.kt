package com.ismartcoding.plain.ui.page.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.grant_permission
import com.ismartcoding.plain.i18n.http_port_conflict_error
import com.ismartcoding.plain.i18n.http_port_conflict_errors
import com.ismartcoding.plain.i18n.http_server_failed
import com.ismartcoding.plain.i18n.system_alert_window_warning
import com.ismartcoding.plain.i18n.vpn_web_conflict_warning
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.httpServerPortsInUse
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.isVPNConnected
import com.ismartcoding.plain.platform.relaunchApp
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.page.MainBottomBar
import com.ismartcoding.plain.ui.page.settings.UpdateDialog
import com.ismartcoding.plain.web.httpPorts
import com.ismartcoding.plain.web.httpsPorts
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

enum class HttpServiceState { OFF, ERROR, ON }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavHostController,
    mainVM: MainViewModel,
    updateVM: UpdateViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    onTabSelected: (Int) -> Unit,
) {
    val serviceEnabled = TempData.serviceEnabled.collectAsStateValue()
    var systemAlertWindow by remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.isGranted()) }
    val refreshState = rememberRefreshLayoutState {
        PeerStatusManager.reconnectNow("home_pull_refresh")
        peerVM.load()
        channelVM.load()
        setRefreshState(RefreshContentState.Finished)
    }
    val scope = rememberCoroutineScope()
    val state = mainVM.httpServerState.value
    var showStayOnlineOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(serviceEnabled) {
        if (serviceEnabled) {
            mainVM.syncHttpServerState()
        }
    }


    val showSuccess = serviceEnabled && state == HttpServerState.ON
    val showLoading = state.isProcessing() || (serviceEnabled && state == HttpServerState.OFF)
    val showError = state == HttpServerState.ERROR
    val errorMessage = buildHomeWebErrorMessage(mainVM)
    val portsInUse = httpServerPortsInUse()

    val onRestartFix: () -> Unit = {
        scope.launch {
            withIO {
                if (portsInUse.contains(TempData.httpPort.value)) {
                    val nextHttp =
                        httpPorts.filter { it != TempData.httpPort.value }.random()
                    HttpPortPreference.putAsync(nextHttp)
                }
                if (portsInUse.contains(TempData.httpsPort.value)) {
                    val nextHttps =
                        httpsPorts.filter { it != TempData.httpsPort.value }.random()
                    HttpsPortPreference.putAsync(nextHttps)
                }
            }
            relaunchApp()
        }
    }

    val httpServiceState = when {
        showSuccess -> HttpServiceState.ON
        showError -> HttpServiceState.ERROR
        else -> HttpServiceState.OFF
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            if (updateVM.consumeUpdateDownloadEvent(event)) {
                return@collect
            }

            when (event) {
                is PermissionsResultEvent -> {
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                }

                is WindowFocusChangedEvent -> {
                    mainVM.isVPNConnected.value = isVPNConnected()
                    val ips = getDeviceIP4s().filter { it.isNotEmpty() }
                    TempData.ip4s.value = ips
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                }
            }
        }
    }

    UpdateDialog(updateVM)

    if (showStayOnlineOverlay) {
        StayOnlineModeOverlay { showStayOnlineOverlay = false }
    }

    PScaffold(
        topBar = { TopBarHome(navController) },
        bottomBar = { MainBottomBar(selectedIndex = 0, onTabSelected = onTabSelected) },
    ) { paddingValues ->
        PullToRefresh(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            refreshLayoutState = refreshState,
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    TopSpace()
                    if (serviceEnabled) {
                        if (mainVM.isVPNConnected.value) {
                            PAlert(
                                description = stringResource(Res.string.vpn_web_conflict_warning),
                                AlertType.WARNING,
                            )
                        }
                        if (!systemAlertWindow) {
                            PAlert(
                                description = stringResource(Res.string.system_alert_window_warning),
                                AlertType.WARNING,
                            ) {
                                PFilledButton(
                                    text = stringResource(Res.string.grant_permission),
                                    buttonSize = ButtonSize.SMALL,
                                    onClick = {
                                        sendEvent(RequestPermissionsEvent(Permission.SYSTEM_ALERT_WINDOW))
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    if (AppFeatureType.CHECK_UPDATES.has()) {
                        UpdateBanner(updateVM)
                    }
                }
                item {
                    AnimatedContent(
                        targetState = httpServiceState,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(200)) using
                                    SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(300) })
                        },
                        label = "web_state",
                    ) { target ->
                        Column {
                            PlainAppServiceSection(
                                navController = navController,
                                mainVM = mainVM,
                                httpServiceState = target,
                                isLoading = showLoading,
                                onRun = {
                                    if (!state.isProcessing() && state != HttpServerState.ON) {
                                        mainVM.enableHttpServer(true)
                                    }
                                },
                                errorMessage = errorMessage,
                                onRestartFix = onRestartFix,
                                onStayOnline = { showStayOnlineOverlay = true },
                            )
                            if (httpServiceState == HttpServiceState.ON) {
                                VerticalSpace(16.dp)
                                DesktopAccessSection(navController)
                                VerticalSpace(dp = 16.dp)
                                DlnaReceiverSection(navController)
                            }
                        }
                    }
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    BottomSpace(paddingValues)
                }
            }
        }
    }
}

private fun buildHomeWebErrorMessage(mainVM: MainViewModel): String {
    val portsInUse = httpServerPortsInUse()
    return if (portsInUse.isNotEmpty()) {
        LocaleHelper.getStringF(
            if (portsInUse.size > 1) Res.string.http_port_conflict_errors else Res.string.http_port_conflict_error,
            "port",
            portsInUse.joinToString(", "),
        )
    } else {
        mainVM.httpServerError.value.ifEmpty { LocaleHelper.getString(Res.string.http_server_failed) }
    }
}